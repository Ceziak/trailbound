package net.ceziak.trailbound.mixin;

import net.ceziak.trailbound.item.PotHoldingHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    /**
     * Cancels attempts to place either pot variant in the
     * player's offhand before Minecraft modifies any stacks.
     *
     * This runs on both the client and server, preventing
     * visual flickering, desynchronisation and duplication.
     */
    @Inject(
            method = "clicked",
            at = @At("HEAD"),
            cancellable = true
    )
    private void trailbound$preventPotInOffhand(
            int slotId,
            int button,
            ClickType clickType,
            Player player,
            CallbackInfo callbackInfo
    ) {
        AbstractContainerMenu menu =
                (AbstractContainerMenu) (Object) this;

        /*
         * Negative slot IDs are special clicks outside the menu.
         */
        if (slotId < 0 || slotId >= menu.slots.size()) {
            return;
        }

        Slot clickedSlot = menu.getSlot(slotId);

        /*
         * Detect the actual player's offhand inventory slot.
         *
         * We check the underlying container slot number rather
         * than the on-screen menu slot number. The player's
         * offhand inventory index is Inventory.SLOT_OFFHAND.
         */
        boolean clickedOffhand =
                clickedSlot.container instanceof Inventory
                        && clickedSlot.getContainerSlot()
                        == Inventory.SLOT_OFFHAND;

        /*
         * Case 1:
         * The player is holding a pot on their cursor and
         * clicks or drags it onto the offhand slot.
         */
        if (clickedOffhand) {
            ItemStack incomingStack = switch (clickType) {
                case PICKUP, QUICK_CRAFT ->
                        menu.getCarried();

                /*
                 * Number-key swapping while hovering over the
                 * offhand slot. Buttons 0-8 represent hotbar
                 * positions.
                 */
                case SWAP -> {
                    if (button >= 0
                            && button < Inventory.getSelectionSize()) {
                        yield player.getInventory().getItem(button);
                    }

                    yield ItemStack.EMPTY;
                }

                default -> ItemStack.EMPTY;
            };

            if (PotHoldingHelper.isPot(incomingStack)) {
                callbackInfo.cancel();
                return;
            }
        }

        /*
         * Case 2:
         * The player hovers over a pot elsewhere in the
         * inventory and presses F.
         *
         * For a container SWAP click, button 40 means the
         * offhand inventory slot.
         */
        if (clickType == ClickType.SWAP
                && button == Inventory.SLOT_OFFHAND
                && PotHoldingHelper.isPot(clickedSlot.getItem())) {
            callbackInfo.cancel();
        }
    }
}