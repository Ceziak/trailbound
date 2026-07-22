package net.ceziak.trailbound.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ceziak.trailbound.Trailbound;
import net.ceziak.trailbound.block.entity.CookingPotBlockEntity;
import net.ceziak.trailbound.item.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class CookingPotHudOverlay {

    /*
     * The panel starts at this width, but expands automatically
     * when translated text or item names require more space.
     */
    private static final int MIN_PANEL_WIDTH = 142;
    private static final int MAX_PANEL_WIDTH = 320;

    private static final int PANEL_HEIGHT = 145;
    private static final int PANEL_X_OFFSET = 36;

    private static final int SCREEN_MARGIN = 4;
    private static final int CONTENT_PADDING = 8;
    private static final int TEXT_GAP = 12;

    private static final int BACKGROUND_RGB = 0x141719;
    private static final int INNER_BACKGROUND_RGB = 0x202426;
    private static final int BORDER_RGB = 0x655C4D;
    private static final int SEPARATOR_RGB = 0x4D4840;

    private static final int TITLE_RGB = 0xF0E5CF;
    private static final int LABEL_RGB = 0xB9B1A2;
    private static final int VALUE_RGB = 0xE5DED1;
    private static final int EMPTY_BAR_RGB = 0x34383A;

    private static final int RECIPE_READY_RGB = 0x79C267;
    private static final int RECIPE_COOKING_RGB = 0xE7AE4A;
    private static final int RECIPE_WAITING_RGB = 0x68A9D2;
    private static final int RECIPE_WARNING_RGB = 0xE07852;
    private static final int RECIPE_INACTIVE_RGB = 0x777777;

    private static final ResourceLocation WATER_SERVING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    Trailbound.MOD_ID,
                    "textures/item/cup_of_water.png"
            );

    private static final float SERVING_CONTAINER_ICON_SCALE = 0.65F;

    private static final int SERVING_CONTAINER_ICON_SIZE =
            Math.round(16.0F * SERVING_CONTAINER_ICON_SCALE);

    private static final int SERVING_CONTAINER_ICON_GAP = 3;

    private static final int WATER_ICON_SIZE = 16;
    private static final int WATER_ICON_GAP = 2;

    private static final int INGREDIENT_SLOT_SIZE = 18;
    private static final int INGREDIENT_SLOT_GAP = 2;

    /*
     * Persistent animation values.
     */
    private static float visibility;
    private static float displayedHeat;
    private static float displayedCooking;

    /*
     * The panel grows immediately to prevent clipping, but
     * shrinks smoothly after shorter content appears.
     */
    private static float displayedPanelWidth =
            MIN_PANEL_WIDTH;

    /*
     * Cached pot information is retained during the fade-out.
     */
    private static BlockPos lastTargetPos;

    private static int lastWaterAmount;
    private static int lastHeatPercentage;
    private static int lastCookingPercentage;

    private static boolean lastReceivingHeat;
    private static boolean lastHasMatchingRecipe;
    private static boolean lastLidStateCorrect;
    private static boolean lastRequiresClosedLid;

    private static ResourceLocation lastActiveRecipeId;

    private static ItemStack lastResult =
            ItemStack.EMPTY;

    private static ItemStack lastServingContainer =
            ItemStack.EMPTY;

    private static final NonNullList<ItemStack> lastIngredients =
            NonNullList.withSize(
                    CookingPotBlockEntity.INGREDIENT_SLOT_COUNT,
                    ItemStack.EMPTY
            );

    public static void render(
            GuiGraphics graphics,
            DeltaTracker deltaTracker
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        CookingPotBlockEntity lookedAtPot =
                findLookedAtPot(minecraft);

        boolean shouldShow =
                lookedAtPot != null
                        && minecraft.screen == null;

        float visibilitySpeed =
                shouldShow ? 0.20F : 0.13F;

        visibility = approach(
                visibility,
                shouldShow ? 1.0F : 0.0F,
                visibilitySpeed
        );

        if (lookedAtPot != null) {
            updateCachedPot(lookedAtPot);
        }

        /*
         * Cut off the nearly invisible final frames so the
         * individual HUD elements disappear together.
         */
        if (visibility < 0.08F) {
            if (!shouldShow) {
                lastTargetPos = null;
            }

            return;
        }

        float easedVisibility =
                visibility
                        * visibility
                        * (3.0F - 2.0F * visibility);

        int alpha = Mth.clamp(
                Math.round(
                        easedVisibility * 255.0F
                ),
                0,
                255
        );

        renderPanel(
                graphics,
                minecraft,
                alpha
        );
    }

    private static void updateCachedPot(
            CookingPotBlockEntity pot
    ) {
        BlockPos currentPos =
                pot.getBlockPos();

        boolean changedTarget =
                lastTargetPos == null
                        || !lastTargetPos.equals(currentPos);

        if (changedTarget) {
            displayedHeat =
                    pot.getHeatPercentage();

            displayedCooking =
                    pot.getCookingPercentage();
        }

        lastTargetPos =
                currentPos.immutable();

        lastWaterAmount =
                pot.getWaterAmount();

        lastHeatPercentage =
                pot.getHeatPercentage();

        lastCookingPercentage =
                pot.getCookingPercentage();

        lastReceivingHeat =
                pot.isReceivingHeat();

        lastHasMatchingRecipe =
                pot.hasMatchingRecipe();

        lastLidStateCorrect =
                pot.isLidStateCorrect();

        lastRequiresClosedLid =
                pot.activeRecipeRequiresClosedLid();

        lastActiveRecipeId =
                pot.getActiveRecipeId();

        lastResult =
                pot.getResult();

        lastServingContainer =
                pot.getRequiredServingContainer();

        displayedHeat = approach(
                displayedHeat,
                lastHeatPercentage,
                0.18F
        );

        displayedCooking = approach(
                displayedCooking,
                lastCookingPercentage,
                0.18F
        );

        /*
         * Copy every slot, including empty ones, so removed
         * ingredients disappear from the HUD immediately.
         */
        for (int slot = 0;
             slot < CookingPotBlockEntity
                     .INGREDIENT_SLOT_COUNT;
             slot++) {

            lastIngredients.set(
                    slot,
                    pot.getIngredient(slot).copy()
            );
        }
    }

    private static CookingPotBlockEntity findLookedAtPot(
            Minecraft minecraft
    ) {
        if (minecraft.level == null
                || minecraft.player == null) {
            return null;
        }

        if (!(minecraft.hitResult
                instanceof BlockHitResult hitResult)) {
            return null;
        }

        if (hitResult.getType()
                != HitResult.Type.BLOCK) {
            return null;
        }

        if (minecraft.level.getBlockEntity(
                hitResult.getBlockPos()
        ) instanceof CookingPotBlockEntity pot) {
            return pot;
        }

        return null;
    }

    private static void renderPanel(
            GuiGraphics graphics,
            Minecraft minecraft,
            int alpha
    ) {
        Font font = minecraft.font;

        int screenWidth =
                graphics.guiWidth();

        int screenHeight =
                graphics.guiHeight();

        int centreX =
                screenWidth / 2;

        int centreY =
                screenHeight / 2;

        /*
         * Limit the width to the available space on one side
         * of the crosshair.
         */
        int availableSideWidth = Math.max(
                1,
                screenWidth / 2
                        - PANEL_X_OFFSET
                        - SCREEN_MARGIN
        );

        int maximumAvailableWidth = Math.max(
                1,
                Math.min(
                        MAX_PANEL_WIDTH,
                        availableSideWidth
                )
        );

        int minimumAllowedWidth = Math.min(
                MIN_PANEL_WIDTH,
                maximumAvailableWidth
        );

        int targetPanelWidth = Math.min(
                calculateTargetPanelWidth(font),
                maximumAvailableWidth
        );

        /*
         * Expand immediately so new text is never briefly
         * clipped. Shrinking remains animated.
         */
        if (targetPanelWidth
                > displayedPanelWidth) {

            displayedPanelWidth =
                    targetPanelWidth;
        } else {
            displayedPanelWidth = approach(
                    displayedPanelWidth,
                    targetPanelWidth,
                    0.18F
            );
        }

        displayedPanelWidth = Mth.clamp(
                displayedPanelWidth,
                minimumAllowedWidth,
                maximumAvailableWidth
        );

        int panelWidth =
                Math.round(displayedPanelWidth);

        int panelX =
                centreX + PANEL_X_OFFSET;

        /*
         * Fall back to the left side when the panel cannot fit
         * on the right.
         */
        if (panelX + panelWidth
                > screenWidth - SCREEN_MARGIN) {

            panelX = centreX
                    - panelWidth
                    - PANEL_X_OFFSET;
        }

        int panelY = Mth.clamp(
                centreY - PANEL_HEIGHT / 2,
                SCREEN_MARGIN,
                Math.max(
                        SCREEN_MARGIN,
                        screenHeight
                                - PANEL_HEIGHT
                                - SCREEN_MARGIN
                )
        );

        boolean panelOnRight =
                panelX > centreX;

        drawConnector(
                graphics,
                centreX,
                centreY,
                panelX,
                panelWidth,
                panelOnRight,
                alpha
        );

        drawPanelBackground(
                graphics,
                panelX,
                panelY,
                panelWidth,
                alpha
        );

        boolean hasWater =
                lastWaterAmount > 0;

        int heatColour =
                hasWater
                        ? getHeatColour(displayedHeat)
                        : EMPTY_BAR_RGB;

        int accentColour =
                !lastResult.isEmpty()
                        ? RECIPE_READY_RGB
                        : heatColour;

        drawPotItemIcon(
                graphics,
                panelX + 8,
                panelY + 6,
                hasWater,
                alpha
        );

        Component title =
                Component.translatable(
                        "gui.trailbound.cooking_pot"
                );

        Component status =
                getStatusComponent();

        graphics.drawString(
                font,
                title,
                panelX + 30,
                panelY + 7,
                argb(alpha, TITLE_RGB),
                true
        );

        graphics.drawString(
                font,
                status,
                panelX + 30,
                panelY + 18,
                argb(
                        alpha,
                        getStatusColour()
                ),
                false
        );

        graphics.fill(
                panelX + 1,
                panelY + 28,
                panelX + panelWidth - 1,
                panelY + 29,
                argb(
                        Math.round(
                                alpha * 0.75F
                        ),
                        accentColour
                )
        );

        drawWaterSection(
                graphics,
                font,
                panelX,
                panelY,
                panelWidth,
                alpha
        );

        drawIngredientSection(
                graphics,
                font,
                panelX,
                panelY,
                alpha
        );

        drawHeatSection(
                graphics,
                minecraft,
                font,
                panelX,
                panelY,
                panelWidth,
                alpha,
                hasWater,
                heatColour
        );

        drawRecipeSection(
                graphics,
                font,
                panelX,
                panelY,
                panelWidth,
                alpha
        );
    }

    private static int calculateTargetPanelWidth(
            Font font
    ) {
        Component title =
                Component.translatable(
                        "gui.trailbound.cooking_pot"
                );

        Component status =
                getStatusComponent();

        Component waterLabel =
                Component.translatable(
                        "gui.trailbound.water"
                );

        Component ingredientsLabel =
                Component.translatable(
                        "gui.trailbound.ingredients"
                );

        Component heatLabel =
                Component.translatable(
                        "gui.trailbound.heat"
                );

        Component recipeLabel =
                Component.translatable(
                        "gui.trailbound.recipe"
                );

        Component recipeStatus =
                getRecipeStatusComponent();

        Component recipeName =
                getRecipeNameComponent();

        int waterIconsWidth =
                CookingPotBlockEntity.MAX_WATER
                        * WATER_ICON_SIZE
                        + (
                        CookingPotBlockEntity.MAX_WATER - 1
                ) * WATER_ICON_GAP;

        int ingredientSlotsWidth =
                CookingPotBlockEntity
                        .INGREDIENT_SLOT_COUNT
                        * INGREDIENT_SLOT_SIZE
                        + (
                        CookingPotBlockEntity
                                .INGREDIENT_SLOT_COUNT - 1
                ) * INGREDIENT_SLOT_GAP;

        int requiredWidth =
                MIN_PANEL_WIDTH;

        /*
         * Header:
         * 30 pixels before the text and normal right padding.
         */
        requiredWidth = Math.max(
                requiredWidth,
                30
                        + Math.max(
                        font.width(title),
                        font.width(status)
                )
                        + CONTENT_PADDING
        );

        /*
         * Water label on the left and serving icons on the
         * right.
         */
        requiredWidth = Math.max(
                requiredWidth,
                CONTENT_PADDING * 2
                        + font.width(waterLabel)
                        + TEXT_GAP
                        + waterIconsWidth
        );

        /*
         * Ingredient label and four item slots.
         */
        requiredWidth = Math.max(
                requiredWidth,
                CONTENT_PADDING * 2
                        + Math.max(
                        font.width(ingredientsLabel),
                        ingredientSlotsWidth
                )
        );

        /*
         * Reserve enough space for the optional exact heat
         * percentage.
         */
        requiredWidth = Math.max(
                requiredWidth,
                CONTENT_PADDING * 2
                        + font.width(heatLabel)
                        + TEXT_GAP
                        + font.width("100%")
        );

        int servingContainerIconWidth =
                !lastResult.isEmpty()
                        && !lastServingContainer.isEmpty()
                        ? SERVING_CONTAINER_ICON_SIZE
                        + SERVING_CONTAINER_ICON_GAP
                        : 0;

        /*
         * Recipe label on the left, with the status and optional
         * serving-container icon on the right.
         */
        requiredWidth = Math.max(
                requiredWidth,
                CONTENT_PADDING * 2
                        + font.width(recipeLabel)
                        + TEXT_GAP
                        + servingContainerIconWidth
                        + font.width(recipeStatus)
        );

        int resultIconWidth =
                !lastResult.isEmpty()
                        ? 20
                        : 0;

        /*
         * Complete recipe or output item name.
         */
        requiredWidth = Math.max(
                requiredWidth,
                CONTENT_PADDING * 2
                        + resultIconWidth
                        + font.width(recipeName)
        );

        return Mth.clamp(
                requiredWidth,
                MIN_PANEL_WIDTH,
                MAX_PANEL_WIDTH
        );
    }

    private static void drawWaterSection(
            GuiGraphics graphics,
            Font font,
            int panelX,
            int panelY,
            int panelWidth,
            int alpha
    ) {
        Component waterLabel =
                Component.translatable(
                        "gui.trailbound.water"
                );

        graphics.drawString(
                font,
                waterLabel,
                panelX + 8,
                panelY + 35,
                argb(alpha, LABEL_RGB),
                false
        );

        drawWaterServings(
                graphics,
                panelX + panelWidth - 8,
                panelY + 30,
                lastWaterAmount,
                alpha
        );
    }

    private static void drawIngredientSection(
            GuiGraphics graphics,
            Font font,
            int panelX,
            int panelY,
            int alpha
    ) {
        Component ingredientsLabel =
                Component.translatable(
                        "gui.trailbound.ingredients"
                );

        graphics.drawString(
                font,
                ingredientsLabel,
                panelX + 8,
                panelY + 49,
                argb(alpha, LABEL_RGB),
                false
        );

        drawIngredientSlots(
                graphics,
                panelX + 8,
                panelY + 59,
                alpha
        );
    }

    private static void drawHeatSection(
            GuiGraphics graphics,
            Minecraft minecraft,
            Font font,
            int panelX,
            int panelY,
            int panelWidth,
            int alpha,
            boolean hasWater,
            int heatColour
    ) {
        Component heatLabel =
                Component.translatable(
                        "gui.trailbound.heat"
                );

        graphics.drawString(
                font,
                heatLabel,
                panelX + 8,
                panelY + 79,
                argb(alpha, LABEL_RGB),
                false
        );

        if (minecraft.player != null
                && minecraft.player.isShiftKeyDown()
                && hasWater) {

            Component percentage =
                    Component.literal(
                            Math.round(
                                    displayedHeat
                            ) + "%"
                    );

            graphics.drawString(
                    font,
                    percentage,
                    panelX + panelWidth
                            - 8
                            - font.width(percentage),
                    panelY + 79,
                    argb(alpha, VALUE_RGB),
                    false
            );
        }

        drawProgressBar(
                graphics,
                panelX + 8,
                panelY + 90,
                panelWidth - 16,
                7,
                alpha,
                hasWater
                        ? Mth.clamp(
                        displayedHeat / 100.0F,
                        0.0F,
                        1.0F
                )
                        : 0.0F,
                heatColour
        );
    }

    private static void drawRecipeSection(
            GuiGraphics graphics,
            Font font,
            int panelX,
            int panelY,
            int panelWidth,
            int alpha
    ) {
        graphics.fill(
                panelX + 1,
                panelY + 104,
                panelX + panelWidth - 1,
                panelY + 105,
                argb(
                        Math.round(
                                alpha * 0.55F
                        ),
                        SEPARATOR_RGB
                )
        );

        Component recipeLabel =
                Component.translatable(
                        "gui.trailbound.recipe"
                );

        Component recipeStatus =
                getRecipeStatusComponent();

        graphics.drawString(
                font,
                recipeLabel,
                panelX + 8,
                panelY + 109,
                argb(alpha, LABEL_RGB),
                false
        );

        int statusX =
                panelX + panelWidth
                        - 8
                        - font.width(recipeStatus);

        graphics.drawString(
                font,
                recipeStatus,
                statusX,
                panelY + 109,
                argb(
                        alpha,
                        getRecipeStatusColour()
                ),
                false
        );

        if (!lastResult.isEmpty()
                && !lastServingContainer.isEmpty()) {

            drawScaledFadingItem(
                    graphics,
                    lastServingContainer,
                    statusX
                            - SERVING_CONTAINER_ICON_SIZE
                            - SERVING_CONTAINER_ICON_GAP,
                    panelY + 108,
                    alpha,
                    SERVING_CONTAINER_ICON_SCALE
            );
        }

        Component recipeName =
                getRecipeNameComponent();

        int nameX =
                panelX + 8;

        if (!lastResult.isEmpty()) {
            drawFadingItem(
                    graphics,
                    lastResult,
                    panelX + 8,
                    panelY + 117,
                    alpha
            );

            nameX =
                    panelX + 28;
        }

        int availableWidth =
                panelX + panelWidth
                        - 8
                        - nameX;

        /*
         * The auto-sized panel should normally fit the complete
         * name. Ellipsis remains as a fallback for tiny screens
         * or exceptionally long translations.
         */
        String displayedName =
                trimWithEllipsis(
                        font,
                        recipeName.getString(),
                        availableWidth
                );

        graphics.drawString(
                font,
                displayedName,
                nameX,
                panelY + 121,
                argb(alpha, VALUE_RGB),
                false
        );

        float progress;

        if (!lastResult.isEmpty()) {
            progress = 1.0F;
        } else if (lastHasMatchingRecipe) {
            progress = Mth.clamp(
                    displayedCooking / 100.0F,
                    0.0F,
                    1.0F
            );
        } else {
            progress = 0.0F;
        }

        drawProgressBar(
                graphics,
                panelX + 8,
                panelY + 134,
                panelWidth - 16,
                7,
                alpha,
                progress,
                getCookingBarColour()
        );
    }

    private static void drawPanelBackground(
            GuiGraphics graphics,
            int x,
            int y,
            int panelWidth,
            int alpha
    ) {
        int borderAlpha =
                Math.round(alpha * 0.75F);

        int backgroundAlpha =
                Math.round(alpha * 0.58F);

        int headerAlpha =
                Math.round(alpha * 0.68F);

        int shadowAlpha =
                Math.round(alpha * 0.28F);

        graphics.fill(
                x + 3,
                y + 3,
                x + panelWidth + 3,
                y + PANEL_HEIGHT + 3,
                argb(shadowAlpha, 0x000000)
        );

        graphics.fill(
                x,
                y,
                x + panelWidth,
                y + PANEL_HEIGHT,
                argb(borderAlpha, BORDER_RGB)
        );

        graphics.fill(
                x + 1,
                y + 1,
                x + panelWidth - 1,
                y + PANEL_HEIGHT - 1,
                argb(
                        backgroundAlpha,
                        BACKGROUND_RGB
                )
        );

        graphics.fill(
                x + 2,
                y + 2,
                x + panelWidth - 2,
                y + 28,
                argb(
                        headerAlpha,
                        INNER_BACKGROUND_RGB
                )
        );
    }

    private static void drawConnector(
            GuiGraphics graphics,
            int centreX,
            int centreY,
            int panelX,
            int panelWidth,
            boolean panelOnRight,
            int alpha
    ) {
        int connectorColour = argb(
                Math.round(alpha * 0.65F),
                BORDER_RGB
        );

        if (panelOnRight) {
            graphics.fill(
                    centreX + 6,
                    centreY,
                    panelX,
                    centreY + 1,
                    connectorColour
            );

            graphics.fill(
                    centreX + 5,
                    centreY - 1,
                    centreX + 7,
                    centreY + 2,
                    connectorColour
            );
        } else {
            graphics.fill(
                    panelX + panelWidth,
                    centreY,
                    centreX - 6,
                    centreY + 1,
                    connectorColour
            );

            graphics.fill(
                    centreX - 7,
                    centreY - 1,
                    centreX - 5,
                    centreY + 2,
                    connectorColour
            );
        }
    }

    private static void drawWaterServings(
            GuiGraphics graphics,
            int rightX,
            int y,
            int waterAmount,
            int alpha
    ) {
        int clampedWater = Mth.clamp(
                waterAmount,
                0,
                CookingPotBlockEntity.MAX_WATER
        );

        int totalWidth =
                CookingPotBlockEntity.MAX_WATER
                        * WATER_ICON_SIZE
                        + (
                        CookingPotBlockEntity.MAX_WATER - 1
                ) * WATER_ICON_GAP;

        int startX =
                rightX - totalWidth;

        for (int serving = 0;
             serving < CookingPotBlockEntity.MAX_WATER;
             serving++) {

            boolean filled =
                    serving < clampedWater;

            drawWaterServingIcon(
                    graphics,
                    startX
                            + serving
                            * (
                            WATER_ICON_SIZE
                                    + WATER_ICON_GAP
                    ),
                    y,
                    alpha,
                    filled
            );
        }
    }

    private static void drawWaterServingIcon(
            GuiGraphics graphics,
            int x,
            int y,
            int alpha,
            boolean filled
    ) {
        if (alpha < 24) {
            return;
        }

        float servingOpacity =
                filled ? 1.0F : 0.16F;

        float finalAlpha = Mth.clamp(
                alpha / 255.0F
                        * servingOpacity,
                0.0F,
                1.0F
        );

        graphics.flush();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                finalAlpha
        );

        try {
            graphics.blit(
                    WATER_SERVING_TEXTURE,
                    x,
                    y,
                    0.0F,
                    0.0F,
                    WATER_ICON_SIZE,
                    WATER_ICON_SIZE,
                    WATER_ICON_SIZE,
                    WATER_ICON_SIZE
            );

            graphics.flush();
        } finally {
            RenderSystem.setShaderColor(
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
        }
    }

    private static void drawIngredientSlots(
            GuiGraphics graphics,
            int startX,
            int y,
            int alpha
    ) {
        int borderAlpha =
                Math.round(alpha * 0.70F);

        int backgroundAlpha =
                Math.round(alpha * 0.48F);

        for (int slot = 0;
             slot < CookingPotBlockEntity
                     .INGREDIENT_SLOT_COUNT;
             slot++) {

            int x = startX
                    + slot
                    * (
                    INGREDIENT_SLOT_SIZE
                            + INGREDIENT_SLOT_GAP
            );

            graphics.fill(
                    x,
                    y,
                    x + INGREDIENT_SLOT_SIZE,
                    y + INGREDIENT_SLOT_SIZE,
                    argb(
                            borderAlpha,
                            BORDER_RGB
                    )
            );

            graphics.fill(
                    x + 1,
                    y + 1,
                    x + INGREDIENT_SLOT_SIZE - 1,
                    y + INGREDIENT_SLOT_SIZE - 1,
                    argb(
                            backgroundAlpha,
                            EMPTY_BAR_RGB
                    )
            );

            graphics.fill(
                    x + 1,
                    y + 1,
                    x + INGREDIENT_SLOT_SIZE - 1,
                    y + 2,
                    argb(
                            Math.round(
                                    alpha * 0.22F
                            ),
                            0xFFFFFF
                    )
            );

            ItemStack ingredient =
                    lastIngredients.get(slot);

            if (!ingredient.isEmpty()) {
                drawFadingItem(
                        graphics,
                        ingredient,
                        x + 1,
                        y + 1,
                        alpha
                );
            }
        }
    }

    private static void drawPotItemIcon(
            GuiGraphics graphics,
            int x,
            int y,
            boolean hasWater,
            int alpha
    ) {
        ItemStack iconStack =
                new ItemStack(
                        hasWater
                                ? ModItems.WATER_POT.get()
                                : ModItems.POT.get()
                );

        drawFadingItem(
                graphics,
                iconStack,
                x,
                y,
                alpha
        );
    }

    private static void drawFadingItem(
            GuiGraphics graphics,
            ItemStack stack,
            int x,
            int y,
            int alpha
    ) {
        if (stack.isEmpty() || alpha < 24) {
            return;
        }

        float itemAlpha = Mth.clamp(
                alpha / 255.0F,
                0.0F,
                1.0F
        );

        graphics.flush();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                itemAlpha
        );

        try {
            graphics.renderItem(
                    stack,
                    x,
                    y
            );

            graphics.flush();
        } finally {
            RenderSystem.setShaderColor(
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
        }
    }

    private static void drawProgressBar(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int alpha,
            float progress,
            int colour
    ) {
        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                argb(alpha, BORDER_RGB)
        );

        graphics.fill(
                x + 1,
                y + 1,
                x + width - 1,
                y + height - 1,
                argb(alpha, EMPTY_BAR_RGB)
        );

        int innerWidth =
                width - 2;

        int filledWidth = Mth.clamp(
                Math.round(
                        innerWidth
                                * Mth.clamp(
                                progress,
                                0.0F,
                                1.0F
                        )
                ),
                0,
                innerWidth
        );

        if (filledWidth <= 0) {
            return;
        }

        graphics.fill(
                x + 1,
                y + 1,
                x + 1 + filledWidth,
                y + height - 1,
                argb(alpha, colour)
        );

        graphics.fill(
                x + 1,
                y + 1,
                x + 1 + filledWidth,
                y + 2,
                argb(
                        Math.round(
                                alpha * 0.70F
                        ),
                        brighten(colour, 28)
                )
        );
    }

    private static Component getStatusComponent() {
        if (!lastResult.isEmpty()) {
            return Component.translatable(
                    "gui.trailbound.recipe.ready"
            );
        }

        if (lastWaterAmount <= 0) {
            return Component.translatable(
                    "gui.trailbound.status.empty"
            );
        }

        if (lastHeatPercentage >= 100) {
            return Component.translatable(
                    "gui.trailbound.status.boiling"
            );
        }

        if (lastHeatPercentage > 0) {
            return Component.translatable(
                    lastReceivingHeat
                            ? "gui.trailbound.status.heating"
                            : "gui.trailbound.status.cooling"
            );
        }

        return Component.translatable(
                "gui.trailbound.status.cold"
        );
    }

    private static int getStatusColour() {
        if (!lastResult.isEmpty()) {
            return RECIPE_READY_RGB;
        }

        if (lastWaterAmount <= 0) {
            return 0x8B8B8B;
        }

        if (lastHeatPercentage >= 100) {
            return 0xF06446;
        }

        if (lastHeatPercentage > 0) {
            return lastReceivingHeat
                    ? 0xE7AE4A
                    : 0x68A9D2;
        }

        return 0x72B8E4;
    }

    private static Component getRecipeStatusComponent() {
        if (!lastResult.isEmpty()) {
            if (!lastServingContainer.isEmpty()) {
                return Component.translatable(
                        "gui.trailbound.recipe.use_container",
                        lastServingContainer.getHoverName()
                );
            }

            return Component.translatable(
                    "gui.trailbound.recipe.ready"
            );
        }

        if (!lastHasMatchingRecipe) {
            return Component.translatable(
                    hasCachedIngredients()
                            ? "gui.trailbound.recipe.invalid"
                            : "gui.trailbound.recipe.add_ingredients"
            );
        }

        if (!lastLidStateCorrect) {
            return Component.translatable(
                    lastRequiresClosedLid
                            ? "gui.trailbound.recipe.close_lid"
                            : "gui.trailbound.recipe.open_lid"
            );
        }

        if (lastHeatPercentage < 100) {
            return Component.translatable(
                    "gui.trailbound.recipe.waiting_for_heat"
            );
        }

        return Component.translatable(
                "gui.trailbound.recipe.cooking"
        );
    }

    private static int getRecipeStatusColour() {
        if (!lastResult.isEmpty()) {
            return RECIPE_READY_RGB;
        }

        if (!lastHasMatchingRecipe) {
            return hasCachedIngredients()
                    ? RECIPE_WARNING_RGB
                    : RECIPE_INACTIVE_RGB;
        }

        if (!lastLidStateCorrect) {
            return RECIPE_WARNING_RGB;
        }

        if (lastHeatPercentage < 100) {
            return RECIPE_WAITING_RGB;
        }

        return RECIPE_COOKING_RGB;
    }

    private static int getCookingBarColour() {
        if (!lastResult.isEmpty()) {
            return RECIPE_READY_RGB;
        }

        if (!lastHasMatchingRecipe) {
            return RECIPE_INACTIVE_RGB;
        }

        if (!lastLidStateCorrect) {
            return RECIPE_WARNING_RGB;
        }

        if (lastHeatPercentage < 100) {
            return RECIPE_WAITING_RGB;
        }

        return RECIPE_COOKING_RGB;
    }

    private static Component getRecipeNameComponent() {
        if (!lastResult.isEmpty()) {
            return lastResult.getHoverName();
        }

        if (lastActiveRecipeId != null) {
            String translationKey =
                    "recipe."
                            + lastActiveRecipeId.getNamespace()
                            + "."
                            + lastActiveRecipeId
                            .getPath()
                            .replace('/', '.');

            String fallback =
                    humanizeRecipePath(
                            lastActiveRecipeId.getPath()
                    );

            return Component.translatableWithFallback(
                    translationKey,
                    fallback
            );
        }

        if (hasCachedIngredients()) {
            return Component.translatable(
                    "gui.trailbound.recipe.unknown"
            );
        }

        return Component.translatable(
                "gui.trailbound.recipe.none"
        );
    }

    private static boolean hasCachedIngredients() {
        for (ItemStack ingredient : lastIngredients) {
            if (!ingredient.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private static void drawScaledFadingItem(
            GuiGraphics graphics,
            ItemStack stack,
            int x,
            int y,
            int alpha,
            float scale
    ) {
        if (stack.isEmpty()
                || alpha < 24
                || scale <= 0.0F) {
            return;
        }

        graphics.pose().pushPose();

        graphics.pose().translate(
                x,
                y,
                0.0F
        );

        graphics.pose().scale(
                scale,
                scale,
                1.0F
        );

        drawFadingItem(
                graphics,
                stack,
                0,
                0,
                alpha
        );

        graphics.pose().popPose();
    }

    private static String humanizeRecipePath(
            String path
    ) {
        int finalSlash =
                path.lastIndexOf('/');

        String finalPart =
                finalSlash >= 0
                        ? path.substring(finalSlash + 1)
                        : path;

        String cleaned =
                finalPart.replace('_', ' ');

        StringBuilder result =
                new StringBuilder();

        boolean capitalizeNext = true;

        for (int index = 0;
             index < cleaned.length();
             index++) {

            char character =
                    cleaned.charAt(index);

            if (Character.isWhitespace(character)) {
                result.append(character);
                capitalizeNext = true;
                continue;
            }

            result.append(
                    capitalizeNext
                            ? Character.toUpperCase(character)
                            : character
            );

            capitalizeNext = false;
        }

        return result.toString();
    }

    private static String trimWithEllipsis(
            Font font,
            String text,
            int width
    ) {
        int safeWidth =
                Math.max(0, width);

        String trimmed =
                font.plainSubstrByWidth(
                        text,
                        safeWidth
                );

        if (trimmed.equals(text)) {
            return trimmed;
        }

        int ellipsisWidth =
                font.width("...");

        return font.plainSubstrByWidth(
                text,
                Math.max(
                        0,
                        safeWidth - ellipsisWidth
                )
        ) + "...";
    }

    private static int getHeatColour(
            float percentage
    ) {
        float clamped = Mth.clamp(
                percentage,
                0.0F,
                100.0F
        );

        if (clamped <= 50.0F) {
            return lerpRgb(
                    0x4C9DD4,
                    0xE0B04C,
                    clamped / 50.0F
            );
        }

        return lerpRgb(
                0xE0B04C,
                0xE4583F,
                (clamped - 50.0F) / 50.0F
        );
    }

    private static int lerpRgb(
            int start,
            int end,
            float amount
    ) {
        amount = Mth.clamp(
                amount,
                0.0F,
                1.0F
        );

        int startRed =
                start >> 16 & 255;

        int startGreen =
                start >> 8 & 255;

        int startBlue =
                start & 255;

        int endRed =
                end >> 16 & 255;

        int endGreen =
                end >> 8 & 255;

        int endBlue =
                end & 255;

        int red = Math.round(
                Mth.lerp(
                        amount,
                        startRed,
                        endRed
                )
        );

        int green = Math.round(
                Mth.lerp(
                        amount,
                        startGreen,
                        endGreen
                )
        );

        int blue = Math.round(
                Mth.lerp(
                        amount,
                        startBlue,
                        endBlue
                )
        );

        return red << 16
                | green << 8
                | blue;
    }

    private static int brighten(
            int colour,
            int amount
    ) {
        int red = Math.min(
                255,
                (colour >> 16 & 255) + amount
        );

        int green = Math.min(
                255,
                (colour >> 8 & 255) + amount
        );

        int blue = Math.min(
                255,
                (colour & 255) + amount
        );

        return red << 16
                | green << 8
                | blue;
    }

    private static float approach(
            float current,
            float target,
            float speed
    ) {
        return current
                + (target - current)
                * speed;
    }

    private static int argb(
            int alpha,
            int rgb
    ) {
        return Mth.clamp(
                alpha,
                0,
                255
        ) << 24
                | rgb & 0xFFFFFF;
    }

    private CookingPotHudOverlay() {
    }
}