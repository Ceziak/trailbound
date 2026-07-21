package net.ceziak.trailbound.event;

import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.item.PotHoldingHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;

@EventBusSubscriber(modid = Trailbound.MOD_ID)
public final class TrailboundCommonEvents {

    /**
     * Blocks the normal F-key hand swap whenever either
     * pot variant is present in one of the hands.
     */
    @SubscribeEvent
    public static void preventPotHandSwap(
            LivingSwapItemsEvent.Hands event
    ) {
        if (PotHoldingHelper.hasPotInEitherHand(
                event.getEntity()
        )) {
            event.setCanceled(true);
        }
    }

    private TrailboundCommonEvents() {
    }
}