package net.ceziak.trailbound.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ceziak.trailbound.block.CookingPotBlock;
import net.ceziak.trailbound.block.entity.CookingPotBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public final class CookingPotBlockEntityRenderer
        implements BlockEntityRenderer<CookingPotBlockEntity> {

    /*
     * Use vanilla's animated still-water texture.
     *
     * Later, we can keep the texture but tint it differently
     * for each tea.
     */
    private static final Material LIQUID_MATERIAL =
            new Material(
                    TextureAtlas.LOCATION_BLOCKS,
                    ResourceLocation.withDefaultNamespace(
                            "block/water_still"
                    )
            );

    /*
     * Your pot's inner opening is approximately:
     *
     * X: 4 to 12
     * Z: 4 to 12
     *
     * We pull the liquid slightly inward to prevent
     * z-fighting against the pot walls.
     */
    private static final float MIN_X = 4.10F / 16.0F;
    private static final float MAX_X = 11.90F / 16.0F;

    private static final float MIN_Z = 4.10F / 16.0F;
    private static final float MAX_Z = 11.90F / 16.0F;

    public CookingPotBlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    @Override
    public void render(
            CookingPotBlockEntity pot,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (!pot.hasWater()) {
            return;
        }

        BlockState state = pot.getBlockState();

        /*
         * Do not draw the liquid through a closed lid.
         */
        if (state.hasProperty(CookingPotBlock.LID)
                && state.getValue(CookingPotBlock.LID)) {
            return;
        }

        float liquidHeight =
                pot.getLiquidRenderHeight() / 16.0F;

        if (liquidHeight <= 0.0F) {
            return;
        }

        int colour = pot.getLiquidColor();

        int red = colour >> 16 & 255;
        int green = colour >> 8 & 255;
        int blue = colour & 255;
        int alpha = pot.getLiquidAlpha();

        TextureAtlasSprite sprite =
                LIQUID_MATERIAL.sprite();

        VertexConsumer consumer =
                LIQUID_MATERIAL.buffer(
                        bufferSource,
                        RenderType::entityTranslucent
                );

        PoseStack.Pose pose = poseStack.last();

        /*
         * Render one horizontal textured quad.
         *
         * The order of these vertices makes the visible side
         * point upward.
         */
        addLiquidVertex(
                consumer,
                pose,
                MIN_X,
                liquidHeight,
                MIN_Z,
                sprite.getU0(),
                sprite.getV0(),
                red,
                green,
                blue,
                alpha,
                packedLight,
                packedOverlay
        );

        addLiquidVertex(
                consumer,
                pose,
                MIN_X,
                liquidHeight,
                MAX_Z,
                sprite.getU0(),
                sprite.getV1(),
                red,
                green,
                blue,
                alpha,
                packedLight,
                packedOverlay
        );

        addLiquidVertex(
                consumer,
                pose,
                MAX_X,
                liquidHeight,
                MAX_Z,
                sprite.getU1(),
                sprite.getV1(),
                red,
                green,
                blue,
                alpha,
                packedLight,
                packedOverlay
        );

        addLiquidVertex(
                consumer,
                pose,
                MAX_X,
                liquidHeight,
                MIN_Z,
                sprite.getU1(),
                sprite.getV0(),
                red,
                green,
                blue,
                alpha,
                packedLight,
                packedOverlay
        );
    }

    private static void addLiquidVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha,
            int packedLight,
            int packedOverlay
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}