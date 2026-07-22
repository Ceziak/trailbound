package net.ceziak.trailbound.client;

import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.item.ModItems;
import net.ceziak.trailbound.item.TeaCupItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(
        modid = Trailbound.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientItemColorEvents {

    private ClientItemColorEvents() {
    }

    @SubscribeEvent
    public static void registerItemColors(
            RegisterColorHandlersEvent.Item event
    ) {
        event.register(
                (stack, tintIndex) -> {
                    /*
                     * layer0: the cup itself.
                     */
                    if (tintIndex == 0) {
                        return 0xFFFFFFFF;
                    }

                    /*
                     * layer1: the tintable tea overlay.
                     */
                    if (tintIndex == 1) {
                        return TeaCupItem
                                .getTeaType(stack)
                                .liquidColor();
                    }

                    return 0xFFFFFFFF;
                },
                ModItems.TEA_CUP.get()
        );
    }
}