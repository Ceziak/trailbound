package net.ceziak.trailbound.mixin.client;

import net.ceziak.trailbound.client.TrailboundClientEnumParams;
import net.ceziak.trailbound.item.PotHoldingHelper;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    /**
     * Reapplies the pot pose after Minecraft finishes all
     * ordinary walking, idle and attack animations.
     */
    @Inject(
            method = "setupAnim",
            at = @At("TAIL")
    )
    private void trailbound$lockPotArmPose(
            LivingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callbackInfo
    ) {
        if (!PotHoldingHelper.isHoldingPotInMainHand(entity)) {
            return;
        }

        HumanoidModel<?> model =
                (HumanoidModel<?>) (Object) this;

        TrailboundClientEnumParams.applyPotHoldPose(
                model,
                entity,
                entity.getMainArm()
        );
    }
}