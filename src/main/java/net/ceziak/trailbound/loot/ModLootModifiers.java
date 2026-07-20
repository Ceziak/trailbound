package net.ceziak.trailbound.loot;

import com.mojang.serialization.MapCodec;
import net.ceziak.trailbound.Trailbound;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>>
            LOOT_MODIFIER_SERIALIZERS = DeferredRegister.create(
            NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
            Trailbound.MOD_ID
    );

    public static final Supplier<MapCodec<LeavesDropModifier>> ADD_LEAVES =
            LOOT_MODIFIER_SERIALIZERS.register(
                    "add_leaves",
                    () -> LeavesDropModifier.CODEC
            );

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(eventBus);
    }

    private ModLootModifiers() {
    }
}