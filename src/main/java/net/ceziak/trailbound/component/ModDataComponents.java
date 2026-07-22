package net.ceziak.trailbound.component;

import com.mojang.serialization.Codec;
import net.ceziak.trailbound.Trailbound;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(
                    Registries.DATA_COMPONENT_TYPE,
                    Trailbound.MOD_ID
            );

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<String>
            > TEA_TYPE =
            DATA_COMPONENTS.registerComponentType(
                    "tea_type",
                    builder -> builder
                            .persistent(Codec.STRING)
                            .networkSynchronized(
                                    ByteBufCodecs.STRING_UTF8
                            )
            );

    public static void register(
            IEventBus eventBus
    ) {
        DATA_COMPONENTS.register(eventBus);
    }

    private ModDataComponents() {
    }
}