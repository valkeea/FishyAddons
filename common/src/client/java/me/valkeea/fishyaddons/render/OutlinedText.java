package me.valkeea.fishyaddons.render;

import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class OutlinedText {

    /**
     * Draw text with a black outline, preserving all style
     * @param text Text to draw
     * @param textColor Color of the text
     */
    public static void withFormat(
        DrawContext context,
        TextRenderer textRenderer,
        Text text,
        int x, int y,
        int textColor
    ) {
        Text clean = TextUtils.stripColor(text);
        outlinedText(context, textRenderer, text, clean, x, y, textColor);
    }

    /**
     * Draw text with a black outline, color style preserved
     * @param text Text to draw
     * @param textColor Color of the text
     */    
    public static void withColor(
        DrawContext context,
        TextRenderer textRenderer,
        Text text,
        int x, int y,
        int textColor
    ) {
        Text clean = TextUtils.stripFormatting(text);
        outlinedText(context, textRenderer, text, clean, x, y, textColor);
    }

    private static void outlinedText(
        DrawContext context,
        TextRenderer textRenderer,
        Text text,
        Text clean,
        int x, int y,
        int textColor
    ) {

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    context.drawText(
                        textRenderer,
                        clean,
                        x + dx,
                        y + dy,
                        0xFF000000,
                        false
                    );
                }
            }
        }

        context.drawText(
            textRenderer,
            text,
            x,
            y,
            textColor,
            false
        );
    }   

    private OutlinedText() {}
}
