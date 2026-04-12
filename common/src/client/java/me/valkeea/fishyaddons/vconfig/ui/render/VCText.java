package me.valkeea.fishyaddons.vconfig.ui.render;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.util.text.GradientRenderer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class VCText {
    
    public static void flatText(DrawContext context, TextRenderer tr, String text, 
                                    int x, int y, int color) {
        context.drawText(tr, text, x, y, color, false);
    }

    public static void flatText(DrawContext context, TextRenderer tr, Text text, 
                                    int x, int y, int color) {
        context.drawText(tr, text, x, y, color, false);
    }

    public static void flatCentered(DrawContext context, TextRenderer tr, String text, 
                                            int centerX, int y, int color) {
        int textWidth = tr.getWidth(text);
        int textX = centerX - textWidth / 2;
        context.drawText(tr, text, textX, y, color, false);
    }

    public static void flatCentered(DrawContext context, TextRenderer tr, Text text, 
                                            int centerX, int y, int color) {
        int textWidth = tr.getWidth(text.copyContentOnly());
        int textX = centerX - textWidth / 2;
        context.drawText(tr, text, textX, y, color, false);
    }

    public static void drawCenteredTextWithShadow(DrawContext context, TextRenderer tr, Text text, 
                                            int centerX, int y, int color) {
        context.drawCenteredTextWithShadow(tr, text, centerX, y, color);
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

    public static int getWidthWithPadding(TextRenderer tr, String text) {
        return tr.getWidth(text) + tr.fontHeight;
    }

    private VCText() {
        throw new UnsupportedOperationException("Utility class");
    }    
}
