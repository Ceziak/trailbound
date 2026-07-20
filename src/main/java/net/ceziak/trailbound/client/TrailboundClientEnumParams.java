package net.ceziak.trailbound.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public final class TrailboundClientEnumParams {

    /*
     * The first constructor argument is true because the pot uses both arms.
     *
     * The second argument is the function that changes the arm rotations.
     */
    public static final EnumProxy<HumanoidModel.ArmPose> POT_HOLD =
            new EnumProxy<>(
                    HumanoidModel.ArmPose.class,
                    true,
                    (IArmPoseTransformer) TrailboundClientEnumParams::applyPotHoldPose
            );

    private static void applyPotHoldPose(
            HumanoidModel<?> model,
            LivingEntity entity,
            HumanoidArm activeArm
    ) {
        /*
         * Bring both arms forward.
         *
         * Negative X rotation raises an arm forward.
         */
        model.rightArm.xRot = degrees(-42.0F);
        model.leftArm.xRot = degrees(-42.0F);

        /*
         * Turn the arms inward toward the middle of the body.
         *
         * The signs are opposite because the arms are mirrored.
         */
        model.rightArm.yRot = degrees(0.0F);
        model.leftArm.yRot = degrees(0.0F);

        /*
         * Slightly roll the arms inward.
         */
        model.rightArm.zRot = degrees(0.0F);
        model.leftArm.zRot = degrees(0.0F);
    }

    private static float degrees(float value) {
        return value * ((float) Math.PI / 180.0F);
    }

    private TrailboundClientEnumParams() {
    }
}