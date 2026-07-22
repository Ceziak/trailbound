package net.ceziak.trailbound.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.item.ModItems;
import net.ceziak.trailbound.recipe.CookingPotRecipe;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;
import java.util.Locale;

public final class CookingPotRecipeCategory
        implements IRecipeCategory<CookingPotRecipe> {

    public static final RecipeType<CookingPotRecipe> RECIPE_TYPE =
            RecipeType.create(
                    Trailbound.MOD_ID,
                    "cooking_pot",
                    CookingPotRecipe.class
            );

    private static final ResourceLocation BACKGROUND_TEXTURE =
            texture("cooking_pot_jei.png");

    private static final ResourceLocation ARROW_TEXTURE =
            texture("cooking_arrow.png");

    private static final ResourceLocation LID_REQUIRED_TEXTURE =
            texture("lid_required.png");

    private static final ResourceLocation LID_OPTIONAL_TEXTURE =
            texture("lid_optional.png");

    private static final ResourceLocation WATER_METER_TEXTURE =
            texture("water_meter.png");

    private static final ResourceLocation WATER_FILL_TEXTURE =
            texture("water_fill.png");

    private static final int WIDTH = 112;
    private static final int HEIGHT = 88;

    /*
     * Slot positions matched to the current 112 x 88 JEI background.
     */
    private static final int[][] INGREDIENT_POSITIONS = {
            {5, 5},
            {24, 5},
            {5, 24},
            {24, 24}
    };

    /*
     * Centred inside the large 26 x 26 result frame.
     */
    private static final int OUTPUT_X = 82;
    private static final int OUTPUT_Y = 16;

    /*
     * Exactly over the grey arrow baked into the background.
     */
    private static final int ARROW_X = 48;
    private static final int ARROW_Y = 15;
    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 17;

    /*
     * Exactly over the small grey lid placeholder.
     */
    private static final int LID_X = 43;
    private static final int LID_Y = 54;
    private static final int LID_WIDTH = 20;
    private static final int LID_HEIGHT = 12;

    /*
     * Exactly over the grey water-drop placeholder.
     */
    private static final int WATER_X = 67;
    private static final int WATER_Y = 54;
    private static final int WATER_WIDTH = 9;
    private static final int WATER_HEIGHT = 12;

    private static final int WATER_VISIBLE_TOP = 2;
    private static final int WATER_VISIBLE_HEIGHT = 8;

    private final IDrawableStatic background;
    private final IDrawable icon;

    public CookingPotRecipeCategory(
            IGuiHelper guiHelper
    ) {
        this.background = guiHelper
                .drawableBuilder(
                        BACKGROUND_TEXTURE,
                        0,
                        0,
                        WIDTH,
                        HEIGHT
                )
                .setTextureSize(WIDTH, HEIGHT)
                .build();

        this.icon = guiHelper.createDrawableItemLike(
                ModItems.POT.get()
        );
    }

    @Override
    public RecipeType<CookingPotRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(
                "jei.trailbound.cooking_pot"
        );
    }

    @Override
    @SuppressWarnings({"deprecation", "removal"})
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            CookingPotRecipe recipe,
            IFocusGroup focuses
    ) {
        List<Ingredient> ingredients =
                recipe.getIngredientList();

        int displayedIngredients = Math.min(
                ingredients.size(),
                INGREDIENT_POSITIONS.length
        );

        for (int index = 0;
             index < displayedIngredients;
             index++) {

            int[] position = INGREDIENT_POSITIONS[index];

            builder.addInputSlot(
                            position[0],
                            position[1]
                    )
                    .addIngredients(ingredients.get(index));
        }

        builder.addOutputSlot(
                        OUTPUT_X,
                        OUTPUT_Y
                )
                .addItemStack(recipe.getResult().copy());

        /*
         * Intentionally no builder.setShapeless(...).
         * We do not want the shapeless crafting icon to appear.
         */
    }

    @Override
    public void draw(
            CookingPotRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        drawCookingArrow(
                graphics,
                recipe.getCookingTime()
        );

        drawLidState(
                graphics,
                recipe.requiresClosedLid()
        );

        drawWaterAmount(
                graphics,
                recipe.getRequiredWater()
        );

        drawCookingTime(
                graphics,
                recipe.getCookingTime()
        );
    }

    @Override
    public boolean needsRecipeBorder() {
        /*
         * The background texture already contains its own
         * panel art.
         */
        return false;
    }

    private static void drawCookingArrow(
            GuiGraphics graphics,
            int cookingTime
    ) {
        graphics.blit(
                ARROW_TEXTURE,
                ARROW_X,
                ARROW_Y,
                0.0F,
                0.0F,
                ARROW_WIDTH,
                ARROW_HEIGHT,
                ARROW_WIDTH,
                ARROW_HEIGHT * 2
        );

        int safeCookingTime = Math.max(1, cookingTime);
        long animationTick = Util.getMillis() / 50L;

        int filledWidth = Mth.clamp(
                (int) (
                        (animationTick % safeCookingTime)
                                * ARROW_WIDTH
                                / safeCookingTime
                ),
                0,
                ARROW_WIDTH
        );

        if (filledWidth <= 0) {
            return;
        }

        graphics.blit(
                ARROW_TEXTURE,
                ARROW_X,
                ARROW_Y,
                0.0F,
                ARROW_HEIGHT,
                filledWidth,
                ARROW_HEIGHT,
                ARROW_WIDTH,
                ARROW_HEIGHT * 2
        );
    }

    private static void drawLidState(
            GuiGraphics graphics,
            boolean requiresClosedLid
    ) {
        ResourceLocation texture = requiresClosedLid
                ? LID_REQUIRED_TEXTURE
                : LID_OPTIONAL_TEXTURE;

        graphics.blit(
                texture,
                LID_X,
                LID_Y,
                0.0F,
                0.0F,
                LID_WIDTH,
                LID_HEIGHT,
                LID_WIDTH,
                LID_HEIGHT
        );
    }

    private static void drawWaterAmount(
            GuiGraphics graphics,
            int requiredWater
    ) {
        int clampedWater = Mth.clamp(
                requiredWater,
                1,
                4
        );

        int fillHeight = Mth.clamp(
                clampedWater * 2,
                2,
                WATER_VISIBLE_HEIGHT
        );

        int sourceY =
                WATER_VISIBLE_TOP
                        + WATER_VISIBLE_HEIGHT
                        - fillHeight;

        graphics.blit(
                WATER_FILL_TEXTURE,
                WATER_X,
                WATER_Y + sourceY,
                0.0F,
                sourceY,
                WATER_WIDTH,
                fillHeight,
                WATER_WIDTH,
                WATER_HEIGHT
        );

        graphics.blit(
                WATER_METER_TEXTURE,
                WATER_X,
                WATER_Y,
                0.0F,
                0.0F,
                WATER_WIDTH,
                WATER_HEIGHT,
                WATER_WIDTH,
                WATER_HEIGHT
        );

        Font font = Minecraft.getInstance().font;

        graphics.drawString(
                font,
                Component.literal("x" + clampedWater),
                WATER_X + 12,
                WATER_Y + 2,
                0xFF373737,
                false
        );
    }

    private static void drawCookingTime(
            GuiGraphics graphics,
            int cookingTime
    ) {
        float seconds = Math.max(1, cookingTime) / 20.0F;

        String timeText =
                seconds == Math.floor(seconds)
                        ? Integer.toString((int) seconds) + "s"
                        : String.format(
                        Locale.ROOT,
                        "%.1fs",
                        seconds
                );

        Font font = Minecraft.getInstance().font;

        graphics.drawString(
                font,
                Component.literal(timeText),
                79,
                68,
                0xFF5A5A5A,
                false
        );
    }

    private static ResourceLocation texture(
            String filename
    ) {
        return ResourceLocation.fromNamespaceAndPath(
                Trailbound.MOD_ID,
                "textures/gui/jei/" + filename
        );
    }
}