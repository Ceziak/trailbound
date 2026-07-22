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

public final class CookingPotBlockEntity extends BlockEntity {

    public static final int MAX_WATER = 4;
    public static final int MAX_HEAT_TICKS = 200;
    public static final int INGREDIENT_SLOT_COUNT = 4;

    private static final int HUD_SYNC_INTERVAL = 5;

    private static final float LOWEST_LIQUID_HEIGHT = 3.25F;
    private static final float LIQUID_HEIGHT_PER_SERVING = 1.0F;

    private final NonNullList<ItemStack> ingredients =
            NonNullList.withSize(
                    INGREDIENT_SLOT_COUNT,
                    ItemStack.EMPTY
            );

    private final NonNullList<ItemStack> resultSourceIngredients =
            NonNullList.withSize(
                    INGREDIENT_SLOT_COUNT,
                    ItemStack.EMPTY
            );

    private ItemStack result = ItemStack.EMPTY;
    private ItemStack resultServingContainer = ItemStack.EMPTY;

    private int waterAmount;
    private int heatProgress;

    private int cookingProgress;
    private int activeCookingTime;
    private ResourceLocation activeRecipeId;
    private boolean activeRecipeRequiresClosedLid;

    private int selectedIngredientSlot = -1;

    public CookingPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOKING_POT.get(), pos, state);
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

        boolean receivingHeat = isActiveHeatSource(
                level.getBlockState(pos.below())
        );

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
                cookingResult != CookingTickResult.NONE;

        boolean anythingChanged =
                heatChanged || cookingChanged;

        boolean heatReachedEndpoint =
                heatChanged
                        && (
                        pot.heatProgress == 0
                                || pot.heatProgress == MAX_HEAT_TICKS
                );

        boolean immediateSync =
                heatReachedEndpoint
                        || cookingResult == CookingTickResult.IMMEDIATE;

        boolean periodicSync =
                level.getGameTime() % HUD_SYNC_INTERVAL == 0;

        if (anythingChanged) {
            pot.setChanged();

            if (immediateSync || periodicSync) {
                pot.sync();
            }
        }

        boolean shouldBeHeated =
                pot.heatProgress >= MAX_HEAT_TICKS;

        if (state.getValue(CookingPotBlock.HEATED) != shouldBeHeated) {
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

    private CookingTickResult tickCooking(ServerLevel level) {
        CookingPotRecipeInput input = createRecipeInput();

        Optional<RecipeHolder<CookingPotRecipe>> match =
                level.getRecipeManager()
                        .getRecipeFor(
                                ModRecipes.COOKING_POT_TYPE.get(),
                                input,
                                level
                        );

        if (match.isEmpty()) {
            return clearActiveRecipeState()
                    ? CookingTickResult.IMMEDIATE
                    : CookingTickResult.NONE;
        }

        RecipeHolder<CookingPotRecipe> holder = match.get();
        CookingPotRecipe recipe = holder.value();

        ItemStack recipeResult = recipe.assemble(
                input,
                level.registryAccess()
        );

        if (!canAcceptResult(recipeResult)) {
            return clearActiveRecipeState()
                    ? CookingTickResult.IMMEDIATE
                    : CookingTickResult.NONE;
        }

        boolean recipeChanged =
                activeRecipeId == null
                        || !activeRecipeId.equals(holder.id());

        boolean timeChanged =
                activeCookingTime != recipe.getCookingTime();

        boolean lidRequirementChanged =
                activeRecipeRequiresClosedLid
                        != recipe.requiresClosedLid();

        if (recipeChanged) {
            activeRecipeId = holder.id();
            cookingProgress = 0;
        }

        if (timeChanged) {
            activeCookingTime = recipe.getCookingTime();
        }

        if (lidRequirementChanged) {
            activeRecipeRequiresClosedLid =
                    recipe.requiresClosedLid();
        }

        boolean activeRecipeChanged =
                recipeChanged
                        || timeChanged
                        || lidRequirementChanged;

        if (!isLidStateCorrect()) {
            return activeRecipeChanged
                    ? CookingTickResult.IMMEDIATE
                    : CookingTickResult.NONE;
        }

        if (!isHot()) {
            return activeRecipeChanged
                    ? CookingTickResult.IMMEDIATE
                    : CookingTickResult.NONE;
        }

        cookingProgress++;

        if (cookingProgress >= activeCookingTime) {
            finishCooking(recipe, input, level);
            return CookingTickResult.IMMEDIATE;
        }

        return CookingTickResult.PROGRESS;
    }

    private void finishCooking(
            CookingPotRecipe recipe,
            CookingPotRecipeInput input,
            ServerLevel level
    ) {
        ItemStack craftedResult = recipe.assemble(
                input,
                level.registryAccess()
        );

        result = craftedResult.copy();
        resultServingContainer = recipe.getServingContainer();

        copyIngredients(
                ingredients,
                resultSourceIngredients
        );

        clearIngredients();

        waterAmount = Mth.clamp(
                waterAmount - recipe.getRequiredWater(),
                0,
                MAX_WATER
        );

        if (waterAmount == 0) {
            heatProgress = 0;
        }

        clearActiveRecipeState();
    }

    private boolean canAcceptResult(ItemStack incoming) {
        return result.isEmpty() && !incoming.isEmpty();
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
        clearItemList(ingredients);
        selectedIngredientSlot = -1;
    }

    private void clearResultSourceIngredients() {
        clearItemList(resultSourceIngredients);
    }

    private static void clearItemList(
            NonNullList<ItemStack> stacks
    ) {
        for (int slot = 0; slot < stacks.size(); slot++) {
            stacks.set(slot, ItemStack.EMPTY);
        }
    }

    private static void copyIngredients(
            NonNullList<ItemStack> source,
            NonNullList<ItemStack> target
    ) {
        clearItemList(target);

        for (int slot = 0; slot < source.size(); slot++) {
            ItemStack stack = source.get(slot);

            target.set(
                    slot,
                    stack.isEmpty()
                            ? ItemStack.EMPTY
                            : stack.copy()
            );
        }
    }

    private static boolean isActiveHeatSource(BlockState state) {
        if (!state.is(ModTags.Blocks.POT_HEAT_SOURCES)) {
            return false;
        }

        if (state.hasProperty(BlockStateProperties.LIT)) {
            return state.getValue(BlockStateProperties.LIT);
        }

        return true;
    }

    public boolean isReceivingHeat() {
        return level != null
                && isActiveHeatSource(
                level.getBlockState(worldPosition.below())
        );
    }

    public int getWaterAmount() {
        return waterAmount;
    }

    public void setWaterAmount(int waterAmount) {
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
                heatProgress * 100.0F / MAX_HEAT_TICKS
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

        /*
         * A false recipe value means the lid is optional, not that the
         * pot must remain open. Only recipes set to true enforce a
         * closed lid.
         */
        if (!activeRecipeRequiresClosedLid) {
            return true;
        }

        return getBlockState().getValue(CookingPotBlock.LID);
    }

    public ItemStack getIngredient(int slot) {
        if (slot < 0 || slot >= ingredients.size()) {
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

    public boolean addIngredient(ItemStack stack) {
        if (stack.isEmpty() || hasResult()) {
            return false;
        }

        for (int slot = 0; slot < ingredients.size(); slot++) {
            if (ingredients.get(slot).isEmpty()) {
                ingredients.set(
                        slot,
                        stack.copyWithCount(1)
                );

                if (selectedIngredientSlot < 0) {
                    selectedIngredientSlot = slot;
                }

                clearActiveRecipeState();
                sync();
                return true;
            }
        }

        return false;
    }

    public int getSelectedIngredientSlot() {
        normalizeSelectedIngredientSlot();
        return selectedIngredientSlot;
    }

    public boolean setSelectedIngredientSlot(int slot) {
        if (!isIngredientSlotOccupied(slot)) {
            return false;
        }

        if (selectedIngredientSlot == slot) {
            return true;
        }

        selectedIngredientSlot = slot;

        if (level == null || !level.isClientSide()) {
            sync();
        }

        return true;
    }

    public boolean isIngredientSlotOccupied(int slot) {
        return slot >= 0
                && slot < ingredients.size()
                && !ingredients.get(slot).isEmpty();
    }

    public int getOccupiedIngredientCount() {
        int occupied = 0;

        for (ItemStack ingredient : ingredients) {
            if (!ingredient.isEmpty()) {
                occupied++;
            }
        }

        return occupied;
    }

    public ItemStack removeSelectedIngredient() {
        normalizeSelectedIngredientSlot();

        if (!isIngredientSlotOccupied(selectedIngredientSlot)) {
            return ItemStack.EMPTY;
        }

        int removedSlot = selectedIngredientSlot;
        ItemStack removed = ingredients.get(removedSlot).copy();

        ingredients.set(removedSlot, ItemStack.EMPTY);

        selectedIngredientSlot = findNextOccupiedSlot(
                removedSlot,
                1
        );

        clearActiveRecipeState();
        sync();
        return removed;
    }

    public ItemStack removeLastIngredient() {
        for (int slot = ingredients.size() - 1; slot >= 0; slot--) {
            if (!isIngredientSlotOccupied(slot)) {
                continue;
            }

            selectedIngredientSlot = slot;
            return removeSelectedIngredient();
        }

        return ItemStack.EMPTY;
    }

    private void normalizeSelectedIngredientSlot() {
        if (isIngredientSlotOccupied(selectedIngredientSlot)) {
            return;
        }

        selectedIngredientSlot = findNextOccupiedSlot(
                -1,
                1
        );
    }

    private int findNextOccupiedSlot(
            int startingSlot,
            int direction
    ) {
        int step = direction < 0 ? -1 : 1;

        for (int offset = 1;
             offset <= ingredients.size();
             offset++) {

            int slot = Math.floorMod(
                    startingSlot + step * offset,
                    ingredients.size()
            );

            if (isIngredientSlotOccupied(slot)) {
                return slot;
            }
        }

        return -1;
    }

    public boolean hasResult() {
        return !result.isEmpty();
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public boolean requiresServingContainer() {
        return hasResult() && !resultServingContainer.isEmpty();
    }

    public ItemStack getRequiredServingContainer() {
        return resultServingContainer.copy();
    }

    public boolean canTakeResultWith(ItemStack heldStack) {
        return hasResult()
                && requiresServingContainer()
                && ItemStack.isSameItemSameComponents(
                heldStack,
                resultServingContainer
        );
    }

    public ItemStack takeResult() {
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack taken = result.copy();

        result = ItemStack.EMPTY;
        resultServingContainer = ItemStack.EMPTY;
        clearResultSourceIngredients();

        sync();
        return taken;
    }

    public ItemStack getResultSourceIngredient(int slot) {
        if (slot < 0 || slot >= resultSourceIngredients.size()) {
            return ItemStack.EMPTY;
        }

        return resultSourceIngredients.get(slot);
    }

    public boolean hasResultSourceIngredients() {
        for (ItemStack ingredient : resultSourceIngredients) {
            if (!ingredient.isEmpty()) {
                return true;
            }
        }

        return false;
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
        super.loadAdditional(tag, registries);

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
                tag.getBoolean("ActiveRecipeRequiresClosedLid");

        clearIngredients();

        ContainerHelper.loadAllItems(
                tag,
                ingredients,
                registries
        );

        selectedIngredientSlot = tag.contains(
                "SelectedIngredientSlot"
        )
                ? tag.getInt("SelectedIngredientSlot")
                : -1;

        normalizeSelectedIngredientSlot();

        clearResultSourceIngredients();

        if (tag.contains("ResultSourceIngredients")) {
            ContainerHelper.loadAllItems(
                    tag.getCompound("ResultSourceIngredients"),
                    resultSourceIngredients,
                    registries
            );
        }

        result = ItemStack.parseOptional(
                registries,
                tag.getCompound("Result")
        );

        resultServingContainer = ItemStack.parseOptional(
                registries,
                tag.getCompound("ResultServingContainer")
        );

        if (result.isEmpty()) {
            resultServingContainer = ItemStack.EMPTY;
            clearResultSourceIngredients();
        }

        activeRecipeId = null;

        String activeRecipe = tag.getString("ActiveRecipe");

        if (!activeRecipe.isEmpty()) {
            activeRecipeId = ResourceLocation.tryParse(activeRecipe);
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
        super.saveAdditional(tag, registries);

        tag.putInt("WaterAmount", waterAmount);
        tag.putInt("HeatProgress", heatProgress);
        tag.putInt("CookingProgress", cookingProgress);
        tag.putInt("ActiveCookingTime", activeCookingTime);

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

        tag.putInt(
                "SelectedIngredientSlot",
                selectedIngredientSlot
        );

        CompoundTag resultSourceTag = new CompoundTag();

        ContainerHelper.saveAllItems(
                resultSourceTag,
                resultSourceIngredients,
                registries
        );

        tag.put(
                "ResultSourceIngredients",
                resultSourceTag
        );

        tag.put(
                "Result",
                result.saveOptional(registries)
        );

        tag.put(
                "ResultServingContainer",
                resultServingContainer.saveOptional(registries)
        );
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries
    ) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        setChanged();

        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();

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