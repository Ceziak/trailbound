package net.ceziak.trailbound.item;

import net.ceziak.trailbound.Trailbound;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Trailbound.MOD_ID);

    public static final DeferredItem<Item> LEAVES =
            ITEMS.register(
                    "leaves",
                    () -> new Item(new Item.Properties())
            );

    public static final DeferredItem<PotItem> POT =
            ITEMS.register(
                    "pot",
                    () -> new PotItem(new Item.Properties())
            );

    public static final DeferredItem<PotItem> WATER_POT =
            ITEMS.register(
                    "pot_of_water",
                    () -> new PotItem(new Item.Properties())
            );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private ModItems() {
    }
}