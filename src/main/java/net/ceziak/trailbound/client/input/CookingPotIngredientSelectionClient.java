package net.ceziak.trailbound.client.input;

import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.block.CookingPotBlock;
import net.ceziak.trailbound.block.entity.CookingPotBlockEntity;
import net.ceziak.trailbound.network.payload.SelectCookingPotIngredientPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Lets the player cycle through ingredients in the looked-at cooking pot.
 */
@EventBusSubscriber(
        modid = Trailbound.MOD_ID,
        value = Dist.CLIENT
)
public final class CookingPotIngredientSelectionClient {

    private CookingPotIngredientSelectionClient() {
    }

    @SubscribeEvent
    public static void onMouseScroll(
            InputEvent.MouseScrollingEvent event
    ) {
        double scrollAmount = event.getScrollDeltaY();

        if (scrollAmount == 0.0D) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen != null
                || minecraft.player == null
                || minecraft.level == null) {
            return;
        }

        if (!(minecraft.hitResult instanceof BlockHitResult hitResult)
                || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        if (!(minecraft.level.getBlockEntity(hitResult.getBlockPos())
                instanceof CookingPotBlockEntity pot)) {
            return;
        }

        if (pot.getBlockState().getValue(CookingPotBlock.LID)
                || pot.hasResult()
                || pot.getOccupiedIngredientCount() < 2) {
            return;
        }

        int currentSlot = pot.getSelectedIngredientSlot();

        /*
         * Wheel up goes to the previous occupied slot;
         * wheel down goes to the next occupied slot.
         */
        int direction = scrollAmount > 0.0D ? -1 : 1;

        int nextSlot = findNextOccupiedSlot(
                pot,
                currentSlot,
                direction
        );

        if (nextSlot < 0 || nextSlot == currentSlot) {
            return;
        }

        /*
         * Update immediately on the client so the GUI feedback feels instant.
         * The packet then validates and stores the same choice server-side.
         */
        pot.setSelectedIngredientSlot(nextSlot);

        PacketDistributor.sendToServer(
                new SelectCookingPotIngredientPayload(
                        hitResult.getBlockPos(),
                        nextSlot
                )
        );

        /*
         * Prevent this particular scroll from changing the hotbar slot.
         */
        event.setCanceled(true);
    }

    private static int findNextOccupiedSlot(
            CookingPotBlockEntity pot,
            int startingSlot,
            int direction
    ) {
        int step = direction < 0 ? -1 : 1;

        for (int offset = 1;
             offset <= CookingPotBlockEntity.INGREDIENT_SLOT_COUNT;
             offset++) {

            int slot = Math.floorMod(
                    startingSlot + step * offset,
                    CookingPotBlockEntity.INGREDIENT_SLOT_COUNT
            );

            if (pot.isIngredientSlotOccupied(slot)) {
                return slot;
            }
        }

        return -1;
    }
}