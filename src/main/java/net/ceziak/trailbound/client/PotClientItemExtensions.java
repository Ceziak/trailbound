package net.ceziak.trailbound.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class PotClientItemExtensions implements IClientItemExtensions {

    public static final PotClientItemExtensions INSTANCE =
            new PotClientItemExtensions();

    private PotClientItemExtensions() {
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(
            LivingEntity entity,
            InteractionHand hand,
            ItemStack stack
    ) {
        InteractionHand otherHand =
                hand == InteractionHand.MAIN_HAND
                        ? InteractionHand.OFF_HAND
                        : InteractionHand.MAIN_HAND;

        // Only use the two-handed pose when the other hand is empty.
        if (!entity.getItemInHand(otherHand).isEmpty()) {
            return null;
        }

        return TrailboundClientEnumParams.POT_HOLD.getValue();
    }
}