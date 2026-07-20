package net.ceziak.trailbound.event;

import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.item.PotHoldingHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Trailbound.MOD_ID)
public final class TrailboundCommonEvents {

    /**
     * Blocks the F-key hand swap whenever either pot variant
     * is currently in one of the player's hands.
     */
    @SubscribeEvent
    public static void preventPotHandSwap(
            LivingSwapItemsEvent.Hands event
    ) {
        if (PotHoldingHelper.hasPotInEitherHand(
                event.getEntity()
        )) {
            event.setCanceled(true);
        }
    }

    /**
     * Safety net for commands, creative-mode oddities or other
     * mods that bypass the normal canEquip restriction.
     *
     * Any pot detected in the offhand is returned to the
     * player's inventory. If the inventory is full, it drops.
     */
    @SubscribeEvent
    public static void removeIllegalOffhandPot(
            PlayerTickEvent.Post event
    ) {
        Player player = event.getEntity();

        if (player.level().isClientSide()) {
            return;
        }

        ItemStack offhandStack = player.getOffhandItem();

        if (!PotHoldingHelper.isPot(offhandStack)) {
            return;
        }

        ItemStack potToReturn = offhandStack.copy();

        player.setItemSlot(
                EquipmentSlot.OFFHAND,
                ItemStack.EMPTY
        );

        if (!player.getInventory().add(potToReturn)) {
            player.drop(potToReturn, false);
        }
    }

    private TrailboundCommonEvents() {
    }
}