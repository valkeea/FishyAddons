package me.valkeea.fishyaddons.vconfig.ui.render;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.util.text.GradientRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class VCText {
    
    public static void flatText(GuiGraphicsExtractor context, Font tr, String text, 
                                    int x, int y, int color) {
        context.text(tr, text, x, y, color, false);
    }

    public static void flatText(GuiGraphicsExtractor context, Font tr, Component text, 
                                    int x, int y, int color) {
        context.text(tr, text, x, y, color, false);
    }

    public static void flatCentered(GuiGraphicsExtractor context, Font tr, String text, 
                                            int centerX, int y, int color) {
        int textWidth = tr.width(text);
        int textX = centerX - textWidth / 2;
        context.text(tr, text, textX, y, color, false);
    }

    public static void flatCentered(GuiGraphicsExtractor context, Font tr, Component text, 
                                            int centerX, int y, int color) {
        int textWidth = tr.width(text.plainCopy());
        int textX = centerX - textWidth / 2;
        context.text(tr, text, textX, y, color, false);
    }

    public static void drawCenteredTextWithShadow(GuiGraphicsExtractor context, Font tr, Component text, 
                                            int centerX, int y, int color) {
        context.centeredText(tr, text, centerX, y, color);
    }

    /**
     * Main (header) gradient
     * @param input The string to apply the gradient to
     * @param style Optional existing text style to apply
     * @return Text with gradient applied
     */
    public static Component header(String input, @Nullable Style style) {
        String gradientDef = "7FFFD4>40E0D0>E0FFFF";
        Style appliedStyle = (style != null) ? style : Style.EMPTY;

        return GradientRenderer.renderCustomGradient(
            input,
            gradientDef,
            appliedStyle
        );
    }

    public static int getWidthWithPadding(Font tr, String text) {
        return tr.width(text) + tr.lineHeight;
    }

    private VCText() {
        throw new UnsupportedOperationException("Utility class");
    }    
}
