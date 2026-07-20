package net.ceziak.trailbound.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.ceziak.trailbound.item.ModItems;
import net.ceziak.trailbound.util.ModTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class LeavesDropModifier extends LootModifier {

    public static final MapCodec<LeavesDropModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    LootModifier.codecStart(instance)
                            .and(
                                    Codec.FLOAT
                                            .fieldOf("chance")
                                            .forGetter(LeavesDropModifier::getChance)
                            )
                            .apply(instance, LeavesDropModifier::new)
            );

    private final float chance;

    public LeavesDropModifier(LootItemCondition[] conditions, float chance) {
        super(conditions);
        this.chance = chance;
    }

    private float getChance() {
        return chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);

        if (state == null) {
            return generatedLoot;
        }

        // Only blocks recognised as leaves.
        if (!state.is(BlockTags.LEAVES)) {
            return generatedLoot;
        }

        // Excludes spruce and any modded needle leaves added to our tag.
        if (state.is(ModTags.Blocks.NEEDLE_LEAVES)) {
            return generatedLoot;
        }

        /*
         * If the leaf block itself is already dropping, the player is
         * probably using shears or Silk Touch. Don't add loose leaves too.
         */
        boolean dropsItself = generatedLoot.stream()
                .anyMatch(stack -> stack.is(state.getBlock().asItem()));

        if (dropsItself) {
            return generatedLoot;
        }

        if (context.getRandom().nextFloat() < chance) {
            int amount = 1 + context.getRandom().nextInt(3);
            generatedLoot.add(new ItemStack(ModItems.LEAVES.get(), amount));
        }

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}