package net.ceziak.trailbound.block.entity;

import net.ceziak.trailbound.block.CookingPotBlock;
import net.ceziak.trailbound.recipe.CookingPotRecipe;
import net.ceziak.trailbound.recipe.CookingPotRecipeInput;
import net.ceziak.trailbound.recipe.ModRecipes;
import net.ceziak.trailbound.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;
import java.util.Optional;

public final class CookingPotBlockEntity
        extends BlockEntity {

    public static final int MAX_WATER = 4;
    public static final int MAX_HEAT_TICKS = 200;
    public static final int INGREDIENT_SLOT_COUNT = 4;

    private static final int HUD_SYNC_INTERVAL = 5;

    /*
     * Physical liquid-surface heights in model pixels.
     */
    private static final float LOWEST_LIQUID_HEIGHT = 3.25F;
    private static final float LIQUID_HEIGHT_PER_SERVING = 1.0F;

    private final NonNullList<ItemStack> ingredients =
            NonNullList.withSize(
                    INGREDIENT_SLOT_COUNT,
                    ItemStack.EMPTY
            );

    private ItemStack result = ItemStack.EMPTY;

    private int waterAmount;
    private int heatProgress;

    /*
     * Cooking progress is separate from water heat.
     */
    private int cookingProgress;
    private int activeCookingTime;

    private ResourceLocation activeRecipeId;

    /*
     * true  = close the lid
     * false = leave the lid open
     */
    private boolean activeRecipeRequiresClosedLid;

    public CookingPotBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.COOKING_POT.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CookingPotBlockEntity pot
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int previousHeat = pot.heatProgress;

        boolean receivingHeat =
                isActiveHeatSource(
                        level.getBlockState(
                                pos.below()
                        )
                );

        /*
         * Heat or cool the water.
         */
        if (pot.hasWater() && receivingHeat) {
            pot.heatProgress = Math.min(
                    MAX_HEAT_TICKS,
                    pot.heatProgress + 1
            );
        } else {
            pot.heatProgress = Math.max(
                    0,
                    pot.heatProgress - 2
            );
        }

        CookingTickResult cookingResult =
                pot.tickCooking(serverLevel);

        boolean heatChanged =
                previousHeat != pot.heatProgress;

        boolean cookingChanged =
                cookingResult
                        != CookingTickResult.NONE;

        boolean anythingChanged =
                heatChanged || cookingChanged;

        /*
         * Only treat an endpoint as newly reached when the heat
         * actually changed this tick. This prevents syncing every
         * tick while the water remains at 100%.
         */
        boolean heatReachedEndpoint =
                heatChanged
                        && (
                        pot.heatProgress == 0
                                || pot.heatProgress
                                == MAX_HEAT_TICKS
                );

        boolean immediateSync =
                heatReachedEndpoint
                        || cookingResult
                        == CookingTickResult.IMMEDIATE;

        boolean periodicSync =
                level.getGameTime()
                        % HUD_SYNC_INTERVAL
                        == 0;

        if (anythingChanged) {
            pot.setChanged();

            if (immediateSync || periodicSync) {
                pot.sync();
            }
        }

        /*
         * Keep the visual HEATED property synchronized with the
         * actual water temperature.
         */
        boolean shouldBeHeated =
                pot.heatProgress >= MAX_HEAT_TICKS;

        if (state.getValue(CookingPotBlock.HEATED)
                != shouldBeHeated) {
            level.setBlock(
                    pos,
                    state.setValue(
                            CookingPotBlock.HEATED,
                            shouldBeHeated
                    ),
                    Block.UPDATE_CLIENTS
            );
        }
    }

    private CookingTickResult tickCooking(
            ServerLevel level
    ) {
        CookingPotRecipeInput input =
                createRecipeInput();

        Optional<RecipeHolder<CookingPotRecipe>> match =
                level.getRecipeManager()
                        .getRecipeFor(
                                ModRecipes
                                        .COOKING_POT_TYPE
                                        .get(),
                                input,
                                level
                        );

        /*
         * Current contents do not match any recipe.
         */
        if (match.isEmpty()) {
            return clearActiveRecipeState()
                    ? CookingTickResult.IMMEDIATE
                    : CookingTickResult.NONE;
        }

        RecipeHolder<CookingPotRecipe> holder =
                match.get();

        CookingPotRecipe recipe =
                holder.value();

        ItemStack recipeResult =
                recipe.assemble(
                        input,
                        level.registryAccess()
                );

        /*
         * Do not start another recipe while its output cannot
         * fit inside the result slot.
         */
        if (!canAcceptResult(recipeResult)) {
            return clearActiveRecipeState()
                    ? CookingTickResult.IMMEDIATE
                    : CookingTickResult.NONE;
        }

        boolean recipeChanged =
                activeRecipeId == null
                        || !activeRecipeId.equals(
                        holder.id()
                );

        boolean timeChanged =
                activeCookingTime
                        != recipe.getCookingTime();

        boolean lidRequirementChanged =
                activeRecipeRequiresClosedLid
                        != recipe.requiresClosedLid();

        if (recipeChanged) {
            activeRecipeId = holder.id();
            cookingProgress = 0;
        }

        if (timeChanged) {
            activeCookingTime =
                    recipe.getCookingTime();
        }

        if (lidRequirementChanged) {
            activeRecipeRequiresClosedLid =
                    recipe.requiresClosedLid();
        }

        boolean activeRecipeChanged =
                recipeChanged
                        || timeChanged
                        || lidRequirementChanged;

        /*
         * The recipe stays detected while the lid is wrong.
         * Cooking simply pauses, allowing the HUD to show an
         * instruction without losing progress.
         */
        if (!isLidStateCorrect()) {
            return activeRecipeChanged
                    ? CookingTickResult.IMMEDIATE
                    : CookingTickResult.NONE;
        }

        /*
         * The cooking timer only advances after the water has
         * reached boiling temperature.
         */
        if (!isHot()) {
            return activeRecipeChanged
                    ? CookingTickResult.IMMEDIATE
                    : CookingTickResult.NONE;
        }

        cookingProgress++;

        if (cookingProgress >= activeCookingTime) {
            finishCooking(
                    recipe,
                    input,
                    level
            );

            return CookingTickResult.IMMEDIATE;
        }

        return CookingTickResult.PROGRESS;
    }

    private void finishCooking(
            CookingPotRecipe recipe,
            CookingPotRecipeInput input,
            ServerLevel level
    ) {
        ItemStack craftedResult =
                recipe.assemble(
                        input,
                        level.registryAccess()
                );

        if (result.isEmpty()) {
            result = craftedResult.copy();
        } else {
            result.grow(
                    craftedResult.getCount()
            );
        }

        /*
         * Matching is exact, so every occupied slot belongs to
         * the completed recipe.
         */
        clearIngredients();

        waterAmount = Mth.clamp(
                waterAmount
                        - recipe.getRequiredWater(),
                0,
                MAX_WATER
        );

        if (waterAmount == 0) {
            heatProgress = 0;
        }

        clearActiveRecipeState();
    }

    private boolean canAcceptResult(
            ItemStack incoming
    ) {
        if (incoming.isEmpty()) {
            return false;
        }

        if (result.isEmpty()) {
            return true;
        }

        if (!ItemStack.isSameItemSameComponents(
                result,
                incoming
        )) {
            return false;
        }

        return result.getCount()
                + incoming.getCount()
                <= result.getMaxStackSize();
    }

    private CookingPotRecipeInput createRecipeInput() {
        return new CookingPotRecipeInput(
                List.copyOf(ingredients),
                waterAmount
        );
    }

    private boolean clearActiveRecipeState() {
        boolean changed =
                activeRecipeId != null
                        || cookingProgress != 0
                        || activeCookingTime != 0
                        || activeRecipeRequiresClosedLid;

        activeRecipeId = null;
        cookingProgress = 0;
        activeCookingTime = 0;
        activeRecipeRequiresClosedLid = false;

        return changed;
    }

    private void clearIngredients() {
        for (int slot = 0;
             slot < ingredients.size();
             slot++) {

            ingredients.set(
                    slot,
                    ItemStack.EMPTY
            );
        }
    }

    private static boolean isActiveHeatSource(
            BlockState state
    ) {
        if (!state.is(
                ModTags.Blocks.POT_HEAT_SOURCES
        )) {
            return false;
        }

        /*
         * Campfires only work while lit. A tagged block without
         * a LIT property is treated as permanently active.
         */
        if (state.hasProperty(
                BlockStateProperties.LIT
        )) {
            return state.getValue(
                    BlockStateProperties.LIT
            );
        }

        return true;
    }

    public boolean isReceivingHeat() {
        return level != null
                && isActiveHeatSource(
                level.getBlockState(
                        worldPosition.below()
                )
        );
    }

    public int getWaterAmount() {
        return waterAmount;
    }

    public void setWaterAmount(
            int waterAmount
    ) {
        int clamped = Mth.clamp(
                waterAmount,
                0,
                MAX_WATER
        );

        if (this.waterAmount == clamped) {
            return;
        }

        this.waterAmount = clamped;

        if (this.waterAmount == 0) {
            heatProgress = 0;
        }

        clearActiveRecipeState();
        sync();
    }

    public boolean hasWater() {
        return waterAmount > 0;
    }

    public int getHeatProgress() {
        return heatProgress;
    }

    public int getHeatPercentage() {
        return Math.round(
                heatProgress
                        * 100.0F
                        / MAX_HEAT_TICKS
        );
    }

    public boolean isHeating() {
        return heatProgress > 0
                && heatProgress < MAX_HEAT_TICKS;
    }

    public boolean isHot() {
        return heatProgress >= MAX_HEAT_TICKS;
    }

    public int getCookingProgress() {
        return cookingProgress;
    }

    public int getCookingTime() {
        return activeCookingTime;
    }

    public int getCookingPercentage() {
        if (activeCookingTime <= 0) {
            return 0;
        }

        return Mth.clamp(
                Math.round(
                        cookingProgress
                                * 100.0F
                                / activeCookingTime
                ),
                0,
                100
        );
    }

    public boolean hasMatchingRecipe() {
        return activeRecipeId != null;
    }

    public boolean isCooking() {
        return activeRecipeId != null
                && isLidStateCorrect()
                && isHot()
                && cookingProgress > 0;
    }

    public ResourceLocation getActiveRecipeId() {
        return activeRecipeId;
    }

    public boolean activeRecipeRequiresClosedLid() {
        return activeRecipeRequiresClosedLid;
    }

    public boolean isLidStateCorrect() {
        if (activeRecipeId == null) {
            return true;
        }

        boolean lidCurrentlyClosed =
                getBlockState().getValue(
                        CookingPotBlock.LID
                );

        return lidCurrentlyClosed
                == activeRecipeRequiresClosedLid;
    }

    public ItemStack getIngredient(
            int slot
    ) {
        if (slot < 0
                || slot >= ingredients.size()) {
            return ItemStack.EMPTY;
        }

        return ingredients.get(slot);
    }

    public boolean hasIngredients() {
        for (ItemStack ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasIngredientSpace() {
        for (ItemStack ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public boolean addIngredient(
            ItemStack stack
    ) {
        if (stack.isEmpty() || hasResult()) {
            return false;
        }

        for (int slot = 0;
             slot < ingredients.size();
             slot++) {

            if (ingredients.get(slot).isEmpty()) {
                ingredients.set(
                        slot,
                        stack.copyWithCount(1)
                );

                clearActiveRecipeState();
                sync();

                return true;
            }
        }

        return false;
    }

    public ItemStack removeLastIngredient() {
        for (int slot =
             ingredients.size() - 1;
             slot >= 0;
             slot--) {

            ItemStack ingredient =
                    ingredients.get(slot);

            if (!ingredient.isEmpty()) {
                ItemStack removed =
                        ingredient.copy();

                ingredients.set(
                        slot,
                        ItemStack.EMPTY
                );

                clearActiveRecipeState();
                sync();

                return removed;
            }
        }

        return ItemStack.EMPTY;
    }

    public boolean hasResult() {
        return !result.isEmpty();
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public ItemStack takeResult() {
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack taken = result.copy();

        result = ItemStack.EMPTY;

        sync();

        return taken;
    }

    public int getLiquidColor() {
        return 0x3F76E4;
    }

    public int getLiquidAlpha() {
        return 190;
    }

    public float getLiquidRenderHeight() {
        if (!hasWater()) {
            return 0.0F;
        }

        return LOWEST_LIQUID_HEIGHT
                + (waterAmount - 1)
                * LIQUID_HEIGHT_PER_SERVING;
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(
                tag,
                registries
        );

        waterAmount = Mth.clamp(
                tag.getInt("WaterAmount"),
                0,
                MAX_WATER
        );

        heatProgress = Mth.clamp(
                tag.getInt("HeatProgress"),
                0,
                MAX_HEAT_TICKS
        );

        cookingProgress = Math.max(
                0,
                tag.getInt("CookingProgress")
        );

        activeCookingTime = Math.max(
                0,
                tag.getInt("ActiveCookingTime")
        );

        activeRecipeRequiresClosedLid =
                tag.getBoolean(
                        "ActiveRecipeRequiresClosedLid"
                );

        /*
         * Clear stale client stacks before loading the newly
         * synchronized ingredient list.
         */
        clearIngredients();

        ContainerHelper.loadAllItems(
                tag,
                ingredients,
                registries
        );

        result = ItemStack.parseOptional(
                registries,
                tag.getCompound("Result")
        );

        activeRecipeId = null;

        String activeRecipe =
                tag.getString("ActiveRecipe");

        if (!activeRecipe.isEmpty()) {
            activeRecipeId =
                    ResourceLocation.tryParse(
                            activeRecipe
                    );
        }

        if (activeRecipeId == null) {
            cookingProgress = 0;
            activeCookingTime = 0;
            activeRecipeRequiresClosedLid = false;
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        tag.putInt(
                "WaterAmount",
                waterAmount
        );

        tag.putInt(
                "HeatProgress",
                heatProgress
        );

        tag.putInt(
                "CookingProgress",
                cookingProgress
        );

        tag.putInt(
                "ActiveCookingTime",
                activeCookingTime
        );

        tag.putBoolean(
                "ActiveRecipeRequiresClosedLid",
                activeRecipeRequiresClosedLid
        );

        if (activeRecipeId != null) {
            tag.putString(
                    "ActiveRecipe",
                    activeRecipeId.toString()
            );
        }

        ContainerHelper.saveAllItems(
                tag,
                ingredients,
                registries
        );

        tag.put(
                "Result",
                result.saveOptional(registries)
        );
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag = new CompoundTag();

        saveAdditional(
                tag,
                registries
        );

        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener>
    getUpdatePacket() {
        return ClientboundBlockEntityDataPacket
                .create(this);
    }

    private void sync() {
        setChanged();

        if (level == null
                || level.isClientSide()) {
            return;
        }

        BlockState state =
                getBlockState();

        level.sendBlockUpdated(
                worldPosition,
                state,
                state,
                Block.UPDATE_CLIENTS
        );
    }

    private enum CookingTickResult {
        NONE,
        PROGRESS,
        IMMEDIATE
    }
}