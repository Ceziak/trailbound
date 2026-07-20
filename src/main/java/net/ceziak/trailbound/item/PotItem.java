package net.ceziak.trailbound.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class PotItem extends Item {

    public PotItem(Properties properties) {
        super(properties);
    }

    /**
     * Prevents this item from being equipped in the offhand slot.
     */
    @Override
    public boolean canEquip(
            ItemStack stack,
            EquipmentSlot slot,
            LivingEntity entity
    ) {
        if (slot == EquipmentSlot.OFFHAND) {
            return false;
        }

        return super.canEquip(stack, slot, entity);
    }
}