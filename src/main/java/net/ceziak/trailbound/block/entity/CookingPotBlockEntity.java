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

    /**
     * Maximum number of servings held by the pot.
     */
    public static final int MAX_WATER = 4;

    /**
     * 200 ticks = 10 seconds.
     */
    public static final int MAX_HEAT_TICKS = 200;

    /**
     * Send a HUD update every five ticks.
     *
     * That is four updates per second, while the HUD smoothly
     * animates between the received values.
     */
    private static final int HUD_SYNC_INTERVAL = 5;

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
        boolean receivingHeat = isActiveHeatSource(
                level.getBlockState(pos.below())
        );

        int previousHeat = pot.heatProgress;

        if (pot.hasWater() && receivingHeat) {
            pot.heatProgress = Math.min(
                    MAX_HEAT_TICKS,
                    pot.heatProgress + 1
            );
        } else {
            /*
             * Cool twice as quickly as the pot heats.
             */
            pot.heatProgress = Math.max(
                    0,
                    pot.heatProgress - 2
            );
        }

        if (previousHeat != pot.heatProgress) {
            pot.setChanged();

            /*
             * Synchronise periodically instead of every tick.
             *
             * Endpoints are always synchronised immediately so
             * the HUD never misses "Cold" or "Boiling".
             */
            boolean reachedEndpoint =
                    pot.heatProgress == 0
                            || pot.heatProgress == MAX_HEAT_TICKS;

            boolean periodicUpdate =
                    level.getGameTime() % HUD_SYNC_INTERVAL == 0;

            if (reachedEndpoint || periodicUpdate) {
                pot.sync();
            }
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

    private static boolean isActiveHeatSource(
            BlockState state
    ) {
        if (!state.is(ModTags.Blocks.POT_HEAT_SOURCES)) {
            return false;
        }

        /*
         * Campfires only produce heat while lit.
         *
         * A tagged block without a LIT property is treated
         * as an always-active heat source.
         */
        if (state.hasProperty(BlockStateProperties.LIT)) {
            return state.getValue(BlockStateProperties.LIT);
        }

        return true;
    }

    /**
     * Can be called on either side.
     *
     * The client uses this to distinguish Heating from Cooling.
     */
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

    public void setWaterAmount(int waterAmount) {
        this.waterAmount = Mth.clamp(
                waterAmount,
                0,
                MAX_WATER
        );

        if (this.waterAmount == 0) {
            heatProgress = 0;
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

        return 6.25F;
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