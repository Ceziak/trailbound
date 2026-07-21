package net.ceziak.trailbound.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class PotItem extends BlockItem {

    private final boolean canBeFilledWithWater;

    public PotItem(
            Block block,
            Properties properties,
            boolean canBeFilledWithWater
    ) {
        super(block, properties);
        this.canBeFilledWithWater = canBeFilledWithWater;
    }

    @Override
    public boolean canEquip(
            ItemStack stack,
            EquipmentSlot slot,
            LivingEntity entity
    ) {
        if (slot == EquipmentSlot.OFFHAND) {
            return false;
        }

        return super.canEquip(stack, slot, entity);
    }

    /**
     * Handles interactions where Minecraft believes the player
     * is clicking a block.
     *
     * We perform our own fluid raycast first because the clicked
     * block may be the solid block behind or underneath the water.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        InteractionHand hand = context.getHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        /*
         * Detect the water the player is actually looking through,
         * rather than relying on context.getClickedPos().
         */
        BlockHitResult waterHit = findTargetedWaterSource(
                level,
                player
        );

        if (waterHit != null) {
            /*
             * The empty pot fills from source water.
             */
            if (canBeFilledWithWater
                    && hand == InteractionHand.MAIN_HAND) {

                BlockPos waterPos = waterHit.getBlockPos();

                if (!level.mayInteract(player, waterPos)) {
                    return InteractionResult.FAIL;
                }

                fillPotFromWater(
                        level,
                        player,
                        hand,
                        waterPos
                );
            }

            /*
             * Always consume an interaction aimed at source water.
             *
             * This also prevents the already-filled water pot
             * from attempting to place itself against the block
             * behind the water.
             */
            return InteractionResult.sidedSuccess(
                    level.isClientSide()
            );
        }

        /*
         * No source water is being targeted.
         *
         * The pot may only be placed while crouching.
         */
        if (!context.isSecondaryUseActive()) {
            return InteractionResult.FAIL;
        }

        /*
         * Crouching while targeting an ordinary solid block:
         * perform normal BlockItem placement.
         */
        return super.useOn(context);
    }

    /**
     * Fallback for interactions where Minecraft reaches Item#use
     * without producing a normal block-use context.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack potStack = player.getItemInHand(hand);

        if (!canBeFilledWithWater
                || hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(potStack);
        }

        BlockHitResult waterHit = findTargetedWaterSource(
                level,
                player
        );

        if (waterHit == null) {
            return InteractionResultHolder.pass(potStack);
        }

        BlockPos waterPos = waterHit.getBlockPos();

        if (!level.mayInteract(player, waterPos)) {
            return InteractionResultHolder.fail(potStack);
        }

        playFillEffects(
                level,
                player,
                waterPos
        );

        ItemStack filledPot =
                ItemUtils.createFilledResult(
                        potStack,
                        player,
                        new ItemStack(
                                ModItems.WATER_POT.get()
                        ),
                        false
                );

        return InteractionResultHolder.sidedSuccess(
                filledPot,
                level.isClientSide()
        );
    }

    /**
     * Raycasts against source fluids independently of the solid
     * block Minecraft may think the player clicked.
     */
    private static BlockHitResult findTargetedWaterSource(
            Level level,
            Player player
    ) {
        BlockHitResult hitResult =
                getPlayerPOVHitResult(
                        level,
                        player,
                        ClipContext.Fluid.SOURCE_ONLY
                );

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        FluidState fluidState =
                level.getFluidState(
                        hitResult.getBlockPos()
                );

        if (!fluidState.is(FluidTags.WATER)
                || !fluidState.isSource()) {
            return null;
        }

        return hitResult;
    }

    /**
     * Used by useOn(), where the resulting stack must be
     * placed into the player's hand manually.
     */
    private void fillPotFromWater(
            Level level,
            Player player,
            InteractionHand hand,
            BlockPos waterPos
    ) {
        /*
         * The server owns the real inventory.
         * The successful client result prevents visual placement.
         */
        if (level.isClientSide()) {
            return;
        }

        ItemStack emptyPot =
                player.getItemInHand(hand);

        playFillEffects(
                level,
                player,
                waterPos
        );

        ItemStack filledPot =
                ItemUtils.createFilledResult(
                        emptyPot,
                        player,
                        new ItemStack(
                                ModItems.WATER_POT.get()
                        ),
                        false
                );

        player.setItemInHand(
                hand,
                filledPot
        );
    }

    private void playFillEffects(
            Level level,
            Player player,
            BlockPos waterPos
    ) {
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.BOTTLE_FILL,
                SoundSource.NEUTRAL,
                1.0F,
                1.0F
        );

        level.gameEvent(
                player,
                GameEvent.FLUID_PICKUP,
                waterPos
        );

        player.awardStat(
                Stats.ITEM_USED.get(this)
        );
    }
}