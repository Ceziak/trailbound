package net.ceziak.trailbound.client;

import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.item.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(
        modid = Trailbound.MOD_ID,
        value = Dist.CLIENT
)
public final class TrailboundClientEvents {

    @SubscribeEvent
    public static void registerClientExtensions(
            RegisterClientExtensionsEvent event
    ) {
        event.registerItem(
                PotClientItemExtensions.INSTANCE,
                ModItems.POT.get()
        );
    }

    private TrailboundClientEvents() {
    }
}