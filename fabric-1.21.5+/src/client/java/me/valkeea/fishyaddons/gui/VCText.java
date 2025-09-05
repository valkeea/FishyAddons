package me.valkeea.fishyaddons.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Text with scaling support
 */
public class VCText {
    
    /**
     * Render text with scaling for smaller GUI scales
     */
    public static void drawScaledText(DrawContext context, TextRenderer textRenderer, String text, 
                                    int x, int y, int color, float uiScale) {
        if (uiScale < 0.7f) {
            context.getMatrices().push();
            context.getMatrices().scale(uiScale, uiScale, 1.0f);
            int scaledX = (int)(x / uiScale);
            int scaledY = (int)(y / uiScale);
            context.drawText(textRenderer, text, scaledX, scaledY, color, false);
            context.getMatrices().pop();
        } else {
            context.drawText(textRenderer, text, x, y, color, false);
        }
    }

    public static void drawScaledButtonText(DrawContext context, TextRenderer textRenderer, String text, 
                                    int x, int y, int color, float uiScale) {
        if (uiScale < 1.2f) {
            context.getMatrices().push();
            context.getMatrices().scale(uiScale, uiScale, 1.0f);
            int scaledX = (int)(x / uiScale);
            int scaledY = (int)(y / uiScale);
                context.drawText(textRenderer, text, scaledX, scaledY, color, false);
            context.getMatrices().pop();
        } else {
            context.drawText(textRenderer, text, x, y, color, false);
        }
    }

    public static void drawScaledCenteredButtonText(DrawContext context, TextRenderer textRenderer, String text, 
                                            int centerX, int y, int color, float uiScale) {
        if (uiScale < 1.2f) {
            context.getMatrices().push();
            context.getMatrices().scale(uiScale, uiScale, 1.0f);
            int scaledCenterX = (int)(centerX / uiScale);
            int scaledY = (int)(y / uiScale);
            context.drawText(textRenderer, text, scaledCenterX, scaledY, color, false);
            context.getMatrices().pop();
        } else {
            context.drawText(textRenderer, text, centerX, y, color, false);
        }
    }

    public static void drawScaledCenteredText(DrawContext context, TextRenderer textRenderer, String text, 
                                            int centerX, int y, int color, float uiScale) {
        if (uiScale < 0.7f) {
            context.getMatrices().push();
            context.getMatrices().scale(uiScale, uiScale, 1.0f);
            int scaledCenterX = (int)(centerX / uiScale);
            int scaledY = (int)(y / uiScale);
            context.drawCenteredTextWithShadow(textRenderer, text, scaledCenterX, scaledY, color);
            context.getMatrices().pop();
        } else {
            context.drawCenteredTextWithShadow(textRenderer, text, centerX, y, color);
        }
    }
    
    /**
     * Calculate scaled text dimensions for layout purposes
     */
    public static int getScaledTextWidth(TextRenderer textRenderer, String text, float uiScale) {
        if (uiScale < 0.7f) {
            return (int)(textRenderer.getWidth(text) * uiScale);
        } else {
            return textRenderer.getWidth(text);
        }
    }
    
    public static int getScaledFontHeight(TextRenderer textRenderer, float uiScale) {
        if (uiScale < 0.7f) {
            return (int)(textRenderer.fontHeight * uiScale);
        } else {
            return textRenderer.fontHeight;
        }
    }

    private VCText() {
        throw new UnsupportedOperationException("Utility class");
    }    
}
