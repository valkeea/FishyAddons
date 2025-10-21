package me.valkeea.fishyaddons.ui;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.util.text.GradientRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class VCText {
    
    public static void drawScaledText(DrawContext context, TextRenderer textRenderer, String text, 
                                    int x, int y, int color, float uiScale) {
        if (uiScale != 1.0f) {
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

    public static void drawScaledText(DrawContext context, TextRenderer textRenderer, Text text, 
                                    int x, int y, int color, float uiScale) {
        if (uiScale != 1.0f) {
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
        if (uiScale != 1.0f) {
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

    public static void drawScaledCenteredText(DrawContext context, TextRenderer textRenderer, net.minecraft.text.Text text, 
                                            int centerX, int y, int color, float uiScale) {
        if (uiScale != 1.0f) {
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
     * Main (header) gradient
     * @param input The string to apply the gradient to
     * @param style Optional existing text style to apply
     * @return Text with gradient applied
     */
    public static Text header(String input, @Nullable Style style) {
        String gradientDef = "7FFFD4>40E0D0>E0FFFF";
        Style appliedStyle = (style != null) ? style : Style.EMPTY;

        return GradientRenderer.renderCustomGradient(
            input,
            gradientDef,
            appliedStyle
        );
    }    
    
    /**
     * Calculate scaled text dimensions for layout purposes
     */
    public static int getScaledTextWidth(TextRenderer textRenderer, String text, float uiScale) {
        if (uiScale != 1.0f) {
            return (int)(textRenderer.getWidth(text) * uiScale);
        } else {
            return textRenderer.getWidth(text);
        }
    }
    
    public static int getScaledFontHeight(TextRenderer textRenderer, float uiScale) {
        if (uiScale != 1.0f) {
            return (int)(textRenderer.fontHeight * uiScale);
        } else {
            return textRenderer.fontHeight;
        }
    }

    private VCText() {
        throw new UnsupportedOperationException("Utility class");
    }    
}
