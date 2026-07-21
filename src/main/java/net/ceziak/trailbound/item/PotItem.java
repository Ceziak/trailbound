package net.ceziak.trailbound.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class PotItem extends Item {

    /*
     * True for the empty pot.
     * False for already-filled pots.
     */
    private final boolean canBeFilledWithWater;

    public PotItem(
            Properties properties,
            boolean canBeFilledWithWater
    ) {
        super(properties);
        this.canBeFilledWithWater = canBeFilledWithWater;
    }

    /**
     * Prevents both pot variants from being equipped
     * in the offhand slot.
     */
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
     * Called when the player right-clicks while holding the item.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack potStack = player.getItemInHand(hand);

        /*
         * The filled water pot uses the same class, but it
         * must not attempt to fill itself again.
         */
        if (!canBeFilledWithWater) {
            return InteractionResultHolder.pass(potStack);
        }

        /*
         * Trailbound pots are main-hand-only.
         *
         * This is also a safety check in case another mod
         * somehow forces one into the offhand.
         */
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(potStack);
        }

        /*
         * Raycast from the player's eyes.
         *
         * SOURCE_ONLY means flowing water will not count;
         * the player must target a full water source block.
         */
        BlockHitResult hitResult = getPlayerPOVHitResult(
                level,
                player,
                ClipContext.Fluid.SOURCE_ONLY
        );

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(potStack);
        }

        BlockPos waterPos = hitResult.getBlockPos();

        /*
         * Respect spawn protection and other interaction
         * restrictions before filling the pot.
         */
        if (!level.mayInteract(player, waterPos)) {
            return InteractionResultHolder.pass(potStack);
        }

        /*
         * Only water-tagged source fluids are accepted.
         *
         * This includes vanilla water and compatible modded
         * fluids that correctly use the water fluid tag.
         */
        if (!level.getFluidState(waterPos).is(FluidTags.WATER)) {
            return InteractionResultHolder.pass(potStack);
        }

        /*
         * Play the same sound used when filling bottles.
         */
        level.playSound(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.BOTTLE_FILL,
                SoundSource.NEUTRAL,
                1.0F,
                1.0F
        );

        /*
         * Notify Minecraft's game-event system that fluid
         * was collected. Sculk sensors and similar systems
         * can react to this.
         */
        level.gameEvent(
                player,
                GameEvent.FLUID_PICKUP,
                waterPos
        );

        /*
         * Count this in the player's item-use statistics.
         */
        player.awardStat(Stats.ITEM_USED.get(this));

        /*
         * Replace one empty pot with a pot of water.
         *
         * ItemUtils also handles creative mode and stacks:
         *
         * - last empty pot -> water pot replaces it in hand
         * - multiple empty pots -> one is consumed and the
         *   filled pot is placed into the inventory
         */
        ItemStack filledPot = ItemUtils.createFilledResult(
                potStack,
                player,
                new ItemStack(ModItems.WATER_POT.get()),
                false
        );

        return InteractionResultHolder.sidedSuccess(
                filledPot,
                level.isClientSide()
        );
    }
}