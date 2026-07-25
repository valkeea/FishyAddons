package me.valkeea.fishyaddons.render;

import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class OutlinedText {

    /**
     * Draw text with a black outline, preserving all style
     * @param text Text to draw
     * @param textColor Color of the text
     */
    public static void withFormat(
        GuiGraphicsExtractor gge,
        Font textRenderer,
        Component text,
        int x, int y,
        int textColor
    ) {
        Component clean = TextUtils.stripColor(text);
        outlinedText(gge, textRenderer, text, clean, x, y, textColor);
    }

    /**
     * Draw text with a black outline, color style preserved
     * @param text Text to draw
     * @param textColor Color of the text
     */    
    public static void withColor(
        GuiGraphicsExtractor gge,
        Font textRenderer,
        Component text,
        int x, int y,
        int textColor
    ) {
        Component clean = TextUtils.stripFormatting(text);
        outlinedText(gge, textRenderer, text, clean, x, y, textColor);
    }

    private static void outlinedText(
        GuiGraphicsExtractor gge,
        Font textRenderer,
        Component text,
        Component clean,
        int x, int y,
        int textColor
    ) {

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    gge.text(
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

        gge.text(
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
