package net.ceziak.trailbound.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ceziak.trailbound.item.PotHoldingHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerItemInHandLayer.class)
public abstract class PlayerItemInHandLayerMixin {

    /**
     * Cancels only the offhand item's third-person render.
     *
     * The player's physical arm remains visible because it is
     * part of the player model rather than this item layer.
     */
    @Inject(
            method = "renderArmWithItem",
            at = @At("HEAD"),
            cancellable = true
    )
    private void trailbound$hideOffhandItemWhileHoldingPot(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext displayContext,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo callbackInfo
    ) {
        if (!PotHoldingHelper.isHoldingPotInMainHand(entity)) {
            return;
        }

        HumanoidArm offhandArm =
                entity.getMainArm() == HumanoidArm.RIGHT
                        ? HumanoidArm.LEFT
                        : HumanoidArm.RIGHT;

        if (arm == offhandArm) {
            callbackInfo.cancel();
        }
    }
}