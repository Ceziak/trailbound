package net.ceziak.trailbound.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.item.ModItems;
import net.ceziak.trailbound.recipe.CookingPotRecipe;
import net.ceziak.trailbound.recipe.ModRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

@JeiPlugin
public final class TrailboundJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(
                    Trailbound.MOD_ID,
                    "jei_plugin"
            );

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(
            IRecipeCategoryRegistration registration
    ) {
        registration.addRecipeCategories(
                new CookingPotRecipeCategory(
                        registration
                                .getJeiHelpers()
                                .getGuiHelper()
                )
        );
    }

    @Override
    public void registerRecipes(
            IRecipeRegistration registration
    ) {
        ClientLevel level =
                Minecraft.getInstance().level;

        if (level == null) {
            return;
        }

        List<CookingPotRecipe> recipes =
                level.getRecipeManager()
                        .getAllRecipesFor(
                                ModRecipes.COOKING_POT_TYPE.get()
                        )
                        .stream()
                        .map(RecipeHolder::value)
                        .toList();

        registration.addRecipes(
                CookingPotRecipeCategory.RECIPE_TYPE,
                recipes
        );
    }

    @Override
    public void registerRecipeCatalysts(
            IRecipeCatalystRegistration registration
    ) {
        registration.addRecipeCatalyst(
                ModItems.POT.get(),
                CookingPotRecipeCategory.RECIPE_TYPE
        );

        registration.addRecipeCatalyst(
                ModItems.WATER_POT.get(),
                CookingPotRecipeCategory.RECIPE_TYPE
        );
    }
}