package net.ceziak.trailbound.block.entity;

import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>>
            BLOCK_ENTITY_TYPES = DeferredRegister.create(
            Registries.BLOCK_ENTITY_TYPE,
            Trailbound.MOD_ID
    );

    public static final Supplier<BlockEntityType<CookingPotBlockEntity>>
            COOKING_POT = BLOCK_ENTITY_TYPES.register(
            "cooking_pot",
            () -> BlockEntityType.Builder.of(
                    CookingPotBlockEntity::new,
                    ModBlocks.POT_BLOCK.get(),
                    ModBlocks.WATER_POT_BLOCK.get()
            ).build(null)
    );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

    private ModBlockEntities() {
    }
}