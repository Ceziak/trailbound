package net.ceziak.trailbound.block;

import net.ceziak.trailbound.Trailbound;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Trailbound.MOD_ID);

    /*
     * Empty pot placed in the world.
     */
    public static final DeferredBlock<CookingPotBlock> POT_BLOCK =
            BLOCKS.register(
                    "pot_block",
                    () -> new CookingPotBlock(potProperties())
            );

    /*
     * Water-filled pot placed in the world.
     *
     * Both block variants use the same Java class and models.
     * Their starting contents and dropped items are different.
     */
    public static final DeferredBlock<CookingPotBlock> WATER_POT_BLOCK =
            BLOCKS.register(
                    "water_pot_block",
                    () -> new CookingPotBlock(potProperties())
            );

    private static BlockBehaviour.Properties potProperties() {
        return BlockBehaviour.Properties.of()
                .strength(0.25F, 2.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private ModBlocks() {
    }
}