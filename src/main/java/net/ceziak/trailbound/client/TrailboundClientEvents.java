package net.ceziak.trailbound.client;

import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.item.ModItems;
import net.ceziak.trailbound.item.PotHoldingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.ceziak.trailbound.block.entity.ModBlockEntities;
import net.ceziak.trailbound.client.renderer.CookingPotBlockEntityRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(
        modid = Trailbound.MOD_ID,
        value = Dist.CLIENT
)
public final class TrailboundClientEvents {

    /**
     * Registers the arm pose for both the empty pot
     * and the pot of water.
     */
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

    /**
     * Hides the offhand item and offhand arm in first person
     * whenever either pot variant is held in the main hand.
     */
    @SubscribeEvent
    public static void hideOffhandInFirstPerson(
            RenderHandEvent event
    ) {
        if (event.getHand() != InteractionHand.OFF_HAND) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        if (PotHoldingHelper.isHoldingPotInMainHand(player)) {
            event.setCanceled(true);
        }
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

    private TrailboundClientEvents() {
    }
}