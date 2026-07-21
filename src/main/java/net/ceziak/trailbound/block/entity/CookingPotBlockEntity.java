package net.ceziak.trailbound.block.entity;

import net.ceziak.trailbound.block.CookingPotBlock;
import net.ceziak.trailbound.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class CookingPotBlockEntity extends BlockEntity {

    /*
     * Later this can represent four cups of tea.
     */
    public static final int MAX_WATER = 4;

    /*
     * 200 game ticks = 10 seconds.
     */
    public static final int MAX_HEAT_TICKS = 200;

    private int waterAmount;
    private int heatProgress;

    public CookingPotBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(ModBlockEntities.COOKING_POT.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CookingPotBlockEntity pot
    ) {
        boolean hasActiveHeat =
                isActiveHeatSource(level.getBlockState(pos.below()));

        int previousHeat = pot.heatProgress;

        if (pot.hasWater() && hasActiveHeat) {
            pot.heatProgress = Math.min(
                    MAX_HEAT_TICKS,
                    pot.heatProgress + 1
            );
        } else {
            /*
             * The pot cools twice as quickly as it heats.
             */
            pot.heatProgress = Math.max(
                    0,
                    pot.heatProgress - 2
            );
        }

        if (previousHeat != pot.heatProgress) {
            pot.setChanged();
        }

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

    /**
     * RGB colour used by the liquid renderer.
     *
     * Later, tea recipes can provide their own colour.
     */
    public int getLiquidColor() {
        return 0x3F76E4;
    }

    /**
     * Transparency from 0 to 255.
     */
    public int getLiquidAlpha() {
        return 190;
    }

    /**
     * Liquid height expressed in model units.
     *
     * Your pot's inner rim is approximately Y = 7.
     */
    public float getLiquidRenderHeight() {
        if (!hasWater()) {
            return 0.0F;
        }

        /*
         * Later, this can change based on servings.
         *
         * For now the full pot surface is at 6.25 model units.
         */
        return 6.25F;
    }

    private static boolean isActiveHeatSource(
            BlockState state
    ) {
        if (!state.is(ModTags.Blocks.POT_HEAT_SOURCES)) {
            return false;
        }

        /*
         * Campfires have a LIT property.
         *
         * Other tagged heat sources without this property
         * are considered permanently active.
         */
        if (state.hasProperty(BlockStateProperties.LIT)) {
            return state.getValue(BlockStateProperties.LIT);
        }

        return true;
    }

    public int getWaterAmount() {
        return waterAmount;
    }

    public void setWaterAmount(int waterAmount) {
        this.waterAmount = Mth.clamp(
                waterAmount,
                0,
                MAX_WATER
        );

        if (this.waterAmount == 0) {
            this.heatProgress = 0;
        }

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

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        waterAmount = tag.getInt("WaterAmount");
        heatProgress = tag.getInt("HeatProgress");
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        tag.putInt("WaterAmount", waterAmount);
        tag.putInt("HeatProgress", heatProgress);
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
}