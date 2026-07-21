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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
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

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        ItemStack potStack =
                player.getItemInHand(hand);

        if (!canBeFilledWithWater
                || hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(potStack);
        }

        BlockHitResult hitResult =
                getPlayerPOVHitResult(
                        level,
                        player,
                        ClipContext.Fluid.SOURCE_ONLY
                );

        if (hitResult.getType()
                != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(potStack);
        }

        BlockPos waterPos = hitResult.getBlockPos();

        if (!level.mayInteract(player, waterPos)) {
            return InteractionResultHolder.pass(potStack);
        }

        if (!level.getFluidState(waterPos)
                .is(FluidTags.WATER)) {
            return InteractionResultHolder.pass(potStack);
        }

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

        level.gameEvent(
                player,
                GameEvent.FLUID_PICKUP,
                waterPos
        );

        player.awardStat(
                Stats.ITEM_USED.get(this)
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
}