package net.ceziak.trailbound.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public record CookingPotRecipeInput(
        List<ItemStack> items,
        int waterAmount
) implements RecipeInput {

    public CookingPotRecipeInput {
        /*
         * Prevent the list structure from being modified while
         * recipes are checking it.
         *
         * The ItemStacks themselves are not changed by recipe
         * matching.
         */
        items = List.copyOf(items);
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= items.size()) {
            throw new IllegalArgumentException(
                    "No cooking-pot item at slot " + slot
            );
        }

        return items.get(slot);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }

        return true;
    }
}