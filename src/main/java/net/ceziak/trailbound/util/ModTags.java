package net.ceziak.trailbound.util;

import net.ceziak.trailbound.Trailbound;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {

    public static final class Blocks {

        public static final TagKey<Block> NEEDLE_LEAVES = createTag("needle_leaves");

        private static TagKey<Block> createTag(String name) {
            return TagKey.create(
                    Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(Trailbound.MOD_ID, name)
            );
        }

        private Blocks() {
        }
    }

    private ModTags() {
    }
}