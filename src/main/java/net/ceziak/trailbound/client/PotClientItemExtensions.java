package net.ceziak.trailbound.client;

import net.ceziak.trailbound.item.PotHoldingHelper;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class PotClientItemExtensions
        implements IClientItemExtensions {

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
        /*
         * The carrying pose is only used when the pot is
         * actually in the main hand.
         */
        if (hand != InteractionHand.MAIN_HAND) {
            return null;
        }

        if (!PotHoldingHelper.isPot(stack)) {
            return null;
        }

        return TrailboundClientEnumParams.POT_HOLD.getValue();
    }
}