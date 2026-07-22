package net.ceziak.trailbound.client.hud;

import net.ceziak.trailbound.block.entity.CookingPotBlockEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.ceziak.trailbound.item.ModItems;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.ceziak.trailbound.Trailbound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.NonNullList;

public final class CookingPotHudOverlay {

    private static final int PANEL_WIDTH = 142;
    private static final int PANEL_HEIGHT = 100;

    private static final int BACKGROUND_RGB = 0x141719;
    private static final int INNER_BACKGROUND_RGB = 0x202426;
    private static final int BORDER_RGB = 0x655C4D;
    private static final int SEPARATOR_RGB = 0x4D4840;

    private static final int TITLE_RGB = 0xF0E5CF;
    private static final int LABEL_RGB = 0xB9B1A2;
    private static final int VALUE_RGB = 0xE5DED1;

    private static final int EMPTY_BAR_RGB = 0x34383A;

    private static final ResourceLocation WATER_SERVING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    Trailbound.MOD_ID,
                    "textures/item/cup_of_water.png"
            );

    private static final int WATER_ICON_SIZE = 16;
    private static final int WATER_ICON_GAP = 2;

    private static final int INGREDIENT_SLOT_COUNT = 4;
    private static final int INGREDIENT_SLOT_SIZE = 18;
    private static final int INGREDIENT_SLOT_GAP = 2;

    /*
     * Persistent animation values.
     */
    private static float visibility;
    private static float displayedHeat;

    /*
     * Last known pot information is retained while the panel
     * fades away.
     */
    private static BlockPos lastTargetPos;
    private static int lastWaterAmount;
    private static int lastHeatPercentage;
    private static boolean lastReceivingHeat;

    private static final NonNullList<ItemStack> lastIngredients =
            NonNullList.withSize(
                    CookingPotBlockEntity
                            .INGREDIENT_SLOT_COUNT,
                    ItemStack.EMPTY
            );

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
                        + (CookingPotBlockEntity.MAX_WATER - 1)
                        * WATER_ICON_GAP;

        int startX = rightX - totalWidth;

        for (int serving = 0;
             serving < CookingPotBlockEntity.MAX_WATER;
             serving++) {

            boolean filled = serving < clampedWater;

            drawWaterServingIcon(
                    graphics,
                    startX + serving
                            * (WATER_ICON_SIZE + WATER_ICON_GAP),
                    y,
                    alpha,
                    filled
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
                    * (INGREDIENT_SLOT_SIZE
                    + INGREDIENT_SLOT_GAP);

            graphics.fill(
                    x,
                    y,
                    x + INGREDIENT_SLOT_SIZE,
                    y + INGREDIENT_SLOT_SIZE,
                    argb(borderAlpha, BORDER_RGB)
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
                            Math.round(alpha * 0.22F),
                            0xFFFFFF
                    )
            );

            ItemStack ingredient =
                    lastIngredients.get(slot);

            if (!ingredient.isEmpty()) {
                drawIngredientItem(
                        graphics,
                        ingredient,
                        x + 1,
                        y + 1,
                        alpha
                );
            }
        }
    }

    private static void drawIngredientItem(
            GuiGraphics graphics,
            ItemStack stack,
            int x,
            int y,
            int alpha
    ) {
        if (alpha < 24) {
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

    private static void drawWaterServingIcon(
            GuiGraphics graphics,
            int x,
            int y,
            int alpha,
            boolean filled
    ) {
        /*
         * Use the same cutoff as the pot icon so all HUD textures
         * disappear at the same moment.
         */
        if (alpha < 24) {
            return;
        }

        float servingOpacity = filled ? 1.0F : 0.16F;

        float finalAlpha = Mth.clamp(
                alpha / 255.0F * servingOpacity,
                0.0F,
                1.0F
        );

        /*
         * Flush around the shader change so its opacity only affects
         * this particular cup icon.
         */
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

    private static void drawPotItemIcon(
            GuiGraphics graphics,
            int x,
            int y,
            boolean hasWater,
            int alpha
    ) {
        /*
         * Stop drawing the icon during the final almost-invisible
         * part of the fade. This prevents stray model pixels.
         */
        if (alpha < 24) {
            return;
        }

        ItemStack iconStack = new ItemStack(
                hasWater
                        ? ModItems.WATER_POT.get()
                        : ModItems.POT.get()
        );

        float iconAlpha = Mth.clamp(
                alpha / 255.0F,
                0.0F,
                1.0F
        );

        /*
         * Flush before changing shader colour so previously queued
         * GUI elements are not affected.
         */
        graphics.flush();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                iconAlpha
        );

        try {
            graphics.renderItem(
                    iconStack,
                    x,
                    y
            );

            /*
             * Force the item to render while our temporary alpha
             * value is still active.
             */
            graphics.flush();
        } finally {
            /*
             * Always restore full opacity or the rest of Minecraft's
             * GUI could also become translucent.
             */
            RenderSystem.setShaderColor(
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );
        }
    }

    public static void render(
            GuiGraphics graphics,
            DeltaTracker deltaTracker
    ) {
        Minecraft minecraft = Minecraft.getInstance();

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
            BlockPos currentPos =
                    lookedAtPot.getBlockPos();

            /*
             * Immediately match the new pot when moving the
             * crosshair from one pot to another.
             */
            if (lastTargetPos == null
                    || !lastTargetPos.equals(currentPos)) {
                displayedHeat =
                        lookedAtPot.getHeatPercentage();
            }

            lastTargetPos = currentPos.immutable();
            lastWaterAmount =
                    lookedAtPot.getWaterAmount();
            lastHeatPercentage =
                    lookedAtPot.getHeatPercentage();
            lastReceivingHeat =
                    lookedAtPot.isReceivingHeat();

            /*
             * Smoothly animate between server updates.
             */
            displayedHeat = approach(
                    displayedHeat,
                    lastHeatPercentage,
                    0.18F
            );

            for (int slot = 0;
                 slot < CookingPotBlockEntity
                         .INGREDIENT_SLOT_COUNT;
                 slot++) {

                lastIngredients.set(
                        slot,
                        lookedAtPot
                                .getIngredient(slot)
                                .copy()
                );
            }
        }

        /*
         * End the fade before very low alpha values cause separate GUI
         * components to become visible at different strengths.
         */
        if (visibility < 0.08F) {
            if (!shouldShow) {
                lastTargetPos = null;
            }

            return;
        }

        /*
         * Smoothstep easing:
         * slower near full visibility, quicker and cleaner near zero.
         */
        /*
         * Smoothstep easing:
         * slower near full visibility, quicker and cleaner near zero.
         */
        float easedVisibility =
                visibility
                        * visibility
                        * (3.0F - 2.0F * visibility);

        int alpha = Mth.clamp(
                Math.round(easedVisibility * 255.0F),
                0,
                255
        );

        renderPanel(
                graphics,
                minecraft,
                alpha
        );
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

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        int centreX = screenWidth / 2;
        int centreY = screenHeight / 2;

        int panelX = centreX + 18;

        /*
         * Move the panel to the left if the right side does
         * not have enough space.
         */
        if (panelX + PANEL_WIDTH > screenWidth - 4) {
            panelX = centreX - PANEL_WIDTH - 18;
        }

        int panelY = Mth.clamp(
                centreY - PANEL_HEIGHT / 2,
                4,
                Math.max(4, screenHeight - PANEL_HEIGHT - 4)
        );

        boolean panelOnRight = panelX > centreX;

        drawConnector(
                graphics,
                centreX,
                centreY,
                panelX,
                panelOnRight,
                alpha
        );

        drawPanelBackground(
                graphics,
                panelX,
                panelY,
                alpha
        );

        boolean hasWater = lastWaterAmount > 0;

        int heatColour = hasWater
                ? getHeatColour(displayedHeat)
                : EMPTY_BAR_RGB;

        drawPotItemIcon(
                graphics,
                panelX + 8,
                panelY + 6,
                hasWater,
                alpha
        );



        Component title = Component.translatable(
                "gui.trailbound.cooking_pot"
        );

        Component status = getStatusComponent();

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

        /*
         * Accent line below the header.
         */
        graphics.fill(
                panelX + 1,
                panelY + 28,
                panelX + PANEL_WIDTH - 1,
                panelY + 29,
                argb(
                        Math.round(alpha * 0.75F),
                        heatColour
                )
        );

        Component waterLabel = Component.translatable(
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

        /*
         * Four cup icons representing the four servings.
         */
        drawWaterServings(
                graphics,
                panelX + PANEL_WIDTH - 8,
                panelY + 30,
                lastWaterAmount,
                alpha
        );

        Component ingredientsLabel = Component.translatable(
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

        Component heatLabel = Component.translatable(
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
            Component percentage = Component.literal(
                    Math.round(displayedHeat) + "%"
            );

            graphics.drawString(
                    font,
                    percentage,
                    panelX + PANEL_WIDTH
                            - 8
                            - font.width(percentage),
                    panelY + 79,
                    argb(alpha, VALUE_RGB),
                    false
            );
        }

        drawHeatBar(
                graphics,
                panelX + 8,
                panelY + 90,
                PANEL_WIDTH - 16,
                7,
                alpha,
                hasWater,
                heatColour
        );
    }

    private static void drawPanelBackground(
            GuiGraphics graphics,
            int x,
            int y,
            int alpha
    ) {
        /*
         * Separate opacity levels:
         *
         * border = fairly visible
         * background = more translucent
         * header = slightly more solid than the main body
         */
        int borderAlpha = Math.round(alpha * 0.75F);
        int backgroundAlpha = Math.round(alpha * 0.58F);
        int headerAlpha = Math.round(alpha * 0.68F);
        int shadowAlpha = Math.round(alpha * 0.28F);

        // Soft blocky shadow.
        graphics.fill(
                x + 3,
                y + 3,
                x + PANEL_WIDTH + 3,
                y + PANEL_HEIGHT + 3,
                argb(shadowAlpha, 0x000000)
        );

        // Outer border.
        graphics.fill(
                x,
                y,
                x + PANEL_WIDTH,
                y + PANEL_HEIGHT,
                argb(borderAlpha, BORDER_RGB)
        );

        // Main translucent body.
        graphics.fill(
                x + 1,
                y + 1,
                x + PANEL_WIDTH - 1,
                y + PANEL_HEIGHT - 1,
                argb(backgroundAlpha, BACKGROUND_RGB)
        );

        // Slightly clearer header section.
        graphics.fill(
                x + 2,
                y + 2,
                x + PANEL_WIDTH - 2,
                y + 28,
                argb(headerAlpha, INNER_BACKGROUND_RGB)
        );
    }

    private static void drawConnector(
            GuiGraphics graphics,
            int centreX,
            int centreY,
            int panelX,
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
                    panelX + PANEL_WIDTH,
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

    private static void drawHeatBar(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int alpha,
            boolean hasWater,
            int heatColour
    ) {
        /*
         * Outer border.
         */
        graphics.fill(
                x,
                y,
                x + width,
                y + height,
                argb(alpha, BORDER_RGB)
        );

        /*
         * Empty inner track.
         */
        graphics.fill(
                x + 1,
                y + 1,
                x + width - 1,
                y + height - 1,
                argb(alpha, EMPTY_BAR_RGB)
        );

        if (!hasWater) {
            return;
        }

        int innerWidth = width - 2;

        int filledWidth = Mth.clamp(
                Math.round(
                        innerWidth
                                * Mth.clamp(
                                displayedHeat / 100.0F,
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
                argb(alpha, heatColour)
        );

        /*
         * Thin highlight across the filled section.
         */
        graphics.fill(
                x + 1,
                y + 1,
                x + 1 + filledWidth,
                y + 2,
                argb(
                        Math.round(alpha * 0.70F),
                        brighten(heatColour, 28)
                )
        );
    }

    private static Component getStatusComponent() {
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

    /**
     * Blue -> amber -> orange-red.
     */
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
        amount = Mth.clamp(amount, 0.0F, 1.0F);

        int startRed = start >> 16 & 255;
        int startGreen = start >> 8 & 255;
        int startBlue = start & 255;

        int endRed = end >> 16 & 255;
        int endGreen = end >> 8 & 255;
        int endBlue = end & 255;

        int red = Math.round(
                Mth.lerp(amount, startRed, endRed)
        );

        int green = Math.round(
                Mth.lerp(amount, startGreen, endGreen)
        );

        int blue = Math.round(
                Mth.lerp(amount, startBlue, endBlue)
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
        return current + (target - current) * speed;
    }

    private static int argb(
            int alpha,
            int rgb
    ) {
        return Mth.clamp(alpha, 0, 255) << 24
                | rgb & 0xFFFFFF;
    }

    private CookingPotHudOverlay() {
    }
}