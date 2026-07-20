package net.ceziak.trailbound.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public final class TrailboundClientEnumParams {

    public static final EnumProxy<HumanoidModel.ArmPose> POT_HOLD =
            new EnumProxy<>(
                    HumanoidModel.ArmPose.class,
                    true,
                    (IArmPoseTransformer)
                            TrailboundClientEnumParams::applyPotHoldPose
            );

    /**
     * Your current working pose:
     *
     * X: -42 degrees
     * Y:   0 degrees
     * Z:   0 degrees
     */
    public static void applyPotHoldPose(
            HumanoidModel<?> model,
            LivingEntity entity,
            HumanoidArm activeArm
    ) {
        model.rightArm.xRot = degrees(-42.0F);
        model.leftArm.xRot = degrees(-42.0F);

        model.rightArm.yRot = degrees(0.0F);
        model.leftArm.yRot = degrees(0.0F);

        model.rightArm.zRot = degrees(0.0F);
        model.leftArm.zRot = degrees(0.0F);
    }

    private static float degrees(float value) {
        return value * ((float) Math.PI / 180.0F);
    }

    private TrailboundClientEnumParams() {
    }
}