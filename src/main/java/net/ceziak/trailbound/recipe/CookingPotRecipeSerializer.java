package net.ceziak.trailbound.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ceziak.trailbound.block.entity.CookingPotBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class CookingPotRecipeSerializer
        implements RecipeSerializer<CookingPotRecipe> {

    public static final MapCodec<CookingPotRecipe> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    instance.group(
                            Ingredient.CODEC_NONEMPTY
                                    .listOf()
                                    .fieldOf("ingredients")
                                    .forGetter(CookingPotRecipe::getIngredientList),

                            Codec.intRange(
                                            1,
                                            CookingPotBlockEntity.MAX_WATER
                                    )
                                    .fieldOf("water")
                                    .forGetter(CookingPotRecipe::getRequiredWater),

                            Codec.intRange(1, 72_000)
                                    .optionalFieldOf("cooking_time", 200)
                                    .forGetter(CookingPotRecipe::getCookingTime),

                            Codec.BOOL
                                    .fieldOf("lid_closed")
                                    .forGetter(CookingPotRecipe::requiresClosedLid),

                            ItemStack.OPTIONAL_CODEC
                                    .optionalFieldOf(
                                            "serving_container",
                                            ItemStack.EMPTY
                                    )
                                    .forGetter(CookingPotRecipe::getServingContainer),

                            ItemStack.STRICT_CODEC
                                    .fieldOf("result")
                                    .forGetter(CookingPotRecipe::getResult)
                    ).apply(instance, CookingPotRecipe::new)
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            CookingPotRecipe
            > STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    @Override
    public MapCodec<CookingPotRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, CookingPotRecipe> streamCodec() {
        return STREAM_CODEC;
    }
}
