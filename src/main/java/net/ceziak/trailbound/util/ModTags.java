package net.ceziak.trailbound.util;

import net.ceziak.trailbound.Trailbound;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;

public final class ModTags {

    public static final class Blocks {

        public static final TagKey<Block> NEEDLE_LEAVES =
                createTag("needle_leaves");

        public static final TagKey<Block> POT_HEAT_SOURCES =
                createTag("pot_heat_sources");

        private static TagKey<Block> createTag(
                String name
        ) {
            return TagKey.create(
                    Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath(
                            Trailbound.MOD_ID,
                            name
                    )
            );
        }

        private Blocks() {
        }
    }

    public static final class Items {

        public static final TagKey<Item> POT_INGREDIENTS =
                createTag("pot_ingredients");

        private static TagKey<Item> createTag(String name) {
            return TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(
                            Trailbound.MOD_ID,
                            name
                    )
            );
        }

        private Items() {
        }
    }

    private ModTags() {
    }
}