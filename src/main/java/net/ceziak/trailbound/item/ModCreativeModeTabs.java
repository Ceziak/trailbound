package net.ceziak.trailbound.item;

import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ceziak.trailbound.data.TeaType;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    Trailbound.MOD_ID
            );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TRAILBOUND =
            CREATIVE_MODE_TABS.register(
                    "trailbound",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.LEAVES.get()))
                            .title(Component.translatable(
                                    "creativetab.trailbound.trailbound"
                            ))
                            .displayItems((parameters, output) -> {
                                // Crops and harvested ingredients
                                output.accept(ModItems.LEAVES.get());
                                output.accept(ModItems.POT.get());
                                output.accept(ModItems.WATER_POT.get());
                                output.accept(ModItems.MARSHMALLOW.get());
                                output.accept(TeaCupItem.create(TeaType.EMPTY));
                                output.accept(TeaCupItem.create(TeaType.GREEN_TEA));
                                output.accept(TeaCupItem.create(TeaType.BERRY_TEA));
                                output.accept(TeaCupItem.create(TeaType.HONEYCOMB_TEA));

                            })
                            .build()
            );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private ModCreativeModeTabs() {
    }
}
