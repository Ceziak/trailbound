package net.ceziak.trailbound.recipe;

import net.ceziak.trailbound.block.entity.CookingPotBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class CookingPotRecipe
        implements Recipe<CookingPotRecipeInput> {

    private final List<Ingredient> ingredients;
    private final int requiredWater;
    private final int cookingTime;
    private final boolean requiresClosedLid;
    private final ItemStack result;

    public CookingPotRecipe(
            List<Ingredient> ingredients,
            int requiredWater,
            int cookingTime,
            boolean requiresClosedLid,
            ItemStack result
    ) {
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException(
                    "A cooking-pot recipe requires at least one ingredient"
            );
        }

        if (ingredients.size()
                > CookingPotBlockEntity.INGREDIENT_SLOT_COUNT) {
            throw new IllegalArgumentException(
                    "A cooking-pot recipe cannot contain more than "
                            + CookingPotBlockEntity.INGREDIENT_SLOT_COUNT
                            + " ingredients"
            );
        }

        if (requiredWater < 1
                || requiredWater
                > CookingPotBlockEntity.MAX_WATER) {
            throw new IllegalArgumentException(
                    "Required water must be between 1 and "
                            + CookingPotBlockEntity.MAX_WATER
            );
        }

        if (cookingTime <= 0) {
            throw new IllegalArgumentException(
                    "Cooking time must be greater than zero"
            );
        }

        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cooking-pot recipe result cannot be empty"
            );
        }

        this.ingredients = List.copyOf(ingredients);
        this.requiredWater = requiredWater;
        this.cookingTime = cookingTime;
        this.requiresClosedLid = requiresClosedLid;
        this.result = result.copy();
    }

    @Override
    public boolean matches(
            CookingPotRecipeInput input,
            Level level
    ) {
        /*
         * Water currently has to match exactly.
         */
        if (input.waterAmount() != requiredWater) {
            return false;
        }

        List<ItemStack> presentItems =
                new ArrayList<>();

        for (int slot = 0;
             slot < input.size();
             slot++) {

            ItemStack stack = input.getItem(slot);

            if (!stack.isEmpty()) {
                presentItems.add(stack);
            }
        }

        /*
         * Prevent recipes from matching when the pot contains
         * additional ingredients not listed by the recipe.
         */
        if (presentItems.size() != ingredients.size()) {
            return false;
        }

        /*
         * Match shapelessly and correctly handle duplicates
         * and ingredients whose possible items overlap.
         */
        return matchIngredient(
                0,
                presentItems,
                new boolean[presentItems.size()]
        );
    }

    private boolean matchIngredient(
            int ingredientIndex,
            List<ItemStack> availableItems,
            boolean[] usedItems
    ) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }

        Ingredient ingredient =
                ingredients.get(ingredientIndex);

        for (int itemIndex = 0;
             itemIndex < availableItems.size();
             itemIndex++) {

            if (usedItems[itemIndex]) {
                continue;
            }

            if (!ingredient.test(
                    availableItems.get(itemIndex)
            )) {
                continue;
            }

            usedItems[itemIndex] = true;

            if (matchIngredient(
                    ingredientIndex + 1,
                    availableItems,
                    usedItems
            )) {
                return true;
            }

            usedItems[itemIndex] = false;
        }

        return false;
    }

    @Override
    public ItemStack assemble(
            CookingPotRecipeInput input,
            HolderLookup.Provider registries
    ) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height
    ) {
        return width * height >= ingredients.size();
    }

    @Override
    public ItemStack getResultItem(
            HolderLookup.Provider registries
    ) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> copy =
                NonNullList.create();

        copy.addAll(ingredients);

        return copy;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COOKING_POT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.COOKING_POT_TYPE.get();
    }

    public List<Ingredient> getIngredientList() {
        return ingredients;
    }

    public int getRequiredWater() {
        return requiredWater;
    }

    public int getCookingTime() {
        return cookingTime;
    }

    public boolean requiresClosedLid() {
        return requiresClosedLid;
    }

    public ItemStack getResult() {
        return result;
    }
}