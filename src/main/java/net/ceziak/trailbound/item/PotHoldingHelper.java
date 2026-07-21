package net.ceziak.trailbound.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class PotHoldingHelper {

    /**
     * Both Trailbound pot variants.
     */
    public static boolean isPot(ItemStack stack) {
        return stack.is(ModItems.POT.get())
                || stack.is(ModItems.WATER_POT.get());
    }

    /**
     * Used by the arm pose and offhand render hiding.
     */
    public static boolean isHoldingPotInMainHand(
            LivingEntity entity
    ) {
        return isPot(entity.getMainHandItem());
    }

    /**
     * Used to block the standard F-key hand swap.
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