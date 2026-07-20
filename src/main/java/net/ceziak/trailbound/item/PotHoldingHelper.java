package net.ceziak.trailbound.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class PotHoldingHelper {

    /**
     * Every item that should use the pot-carrying system.
     */
    public static boolean isPot(ItemStack stack) {
        return stack.is(ModItems.POT.get())
                || stack.is(ModItems.WATER_POT.get());
    }

    /**
     * True whenever either pot variant is in the main hand.
     *
     * The offhand may contain an item because that item will
     * be hidden while the pot is being carried.
     */
    public static boolean isHoldingPotInMainHand(
            LivingEntity entity
    ) {
        return isPot(entity.getMainHandItem());
    }

    /**
     * Used for blocking the F-key hand swap.
     */
    public static boolean hasPotInEitherHand(
            LivingEntity entity
    ) {
        return isPot(entity.getMainHandItem())
                || isPot(entity.getOffhandItem());
    }

    private PotHoldingHelper() {
    }
}