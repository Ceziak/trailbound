package net.ceziak.trailbound.network;

import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.block.CookingPotBlock;
import net.ceziak.trailbound.block.entity.CookingPotBlockEntity;
import net.ceziak.trailbound.network.payload.SelectCookingPotIngredientPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers Trailbound's custom network payloads.
 */
@EventBusSubscriber(modid = Trailbound.MOD_ID)

public final class ModPayloads {

    private static final double MAX_INTERACTION_DISTANCE_SQUARED = 64.0D;

    private ModPayloads() {
    }

    @SubscribeEvent
    public static void registerPayloads(
            RegisterPayloadHandlersEvent event
    ) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                SelectCookingPotIngredientPayload.TYPE,
                SelectCookingPotIngredientPayload.STREAM_CODEC,
                ModPayloads::handleIngredientSelection
        );
    }

    private static void handleIngredientSelection(
            SelectCookingPotIngredientPayload payload,
            IPayloadContext context
    ) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }

        BlockPos pos = payload.potPos();
        ServerLevel level = player.serverLevel();

        if (!level.hasChunkAt(pos)) {
            return;
        }

        double distanceSquared = player.distanceToSqr(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );

        if (distanceSquared > MAX_INTERACTION_DISTANCE_SQUARED) {
            return;
        }

        if (!(level.getBlockEntity(pos)
                instanceof CookingPotBlockEntity pot)) {
            return;
        }

        if (pot.getBlockState().getValue(CookingPotBlock.LID)
                || pot.hasResult()) {
            return;
        }

        pot.setSelectedIngredientSlot(
                payload.ingredientSlot()
        );
    }
}