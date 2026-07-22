package net.ceziak.trailbound.client;

import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.block.entity.ModBlockEntities;
import net.ceziak.trailbound.client.hud.CookingPotHudOverlay;
import net.ceziak.trailbound.client.renderer.CookingPotBlockEntityRenderer;
import net.ceziak.trailbound.item.ModItems;
import net.ceziak.trailbound.item.PotHoldingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(
        modid = Trailbound.MOD_ID,
        value = Dist.CLIENT
)
public final class TrailboundClientEvents {

    private static final ResourceLocation COOKING_POT_HUD =
            ResourceLocation.fromNamespaceAndPath(
                    Trailbound.MOD_ID,
                    "cooking_pot_hud"
            );

    @SubscribeEvent
    public static void registerClientExtensions(
            RegisterClientExtensionsEvent event
    ) {
        event.registerItem(
                PotClientItemExtensions.INSTANCE,
                ModItems.POT.get(),
                ModItems.WATER_POT.get()
        );
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.COOKING_POT.get(),
                CookingPotBlockEntityRenderer::new
        );
    }

    /**
     * Render immediately after the vanilla crosshair layer.
     */
    @SubscribeEvent
    public static void registerGuiLayers(
            RegisterGuiLayersEvent event
    ) {
        event.registerAbove(
                VanillaGuiLayers.CROSSHAIR,
                COOKING_POT_HUD,
                CookingPotHudOverlay::render
        );
    }

    @SubscribeEvent
    public static void hideOffhandInFirstPerson(
            RenderHandEvent event
    ) {
        if (event.getHand()
                != InteractionHand.OFF_HAND) {
            return;
        }

        LocalPlayer player =
                Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        if (PotHoldingHelper
                .isHoldingPotInMainHand(player)) {
            event.setCanceled(true);
        }
    }

    private TrailboundClientEvents() {
    }
}