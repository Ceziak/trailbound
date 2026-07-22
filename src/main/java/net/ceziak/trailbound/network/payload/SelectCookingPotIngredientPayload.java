package net.ceziak.trailbound.network.payload;

import net.ceziak.trailbound.Trailbound;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from the client when the player changes the ingredient selected
 * in a cooking pot with the mouse wheel.
 */
public record SelectCookingPotIngredientPayload(
        BlockPos potPos,
        int ingredientSlot
) implements CustomPacketPayload {

    public static final Type<SelectCookingPotIngredientPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            Trailbound.MOD_ID,
                            "select_cooking_pot_ingredient"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SelectCookingPotIngredientPayload
            > STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SelectCookingPotIngredientPayload::potPos,
            ByteBufCodecs.VAR_INT,
            SelectCookingPotIngredientPayload::ingredientSlot,
            SelectCookingPotIngredientPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}