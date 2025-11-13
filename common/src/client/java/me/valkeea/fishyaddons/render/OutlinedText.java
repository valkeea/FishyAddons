package me.valkeea.fishyaddons.render;

import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.client.MinecraftClient;
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
        float x, float y,
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
        float x, float y,
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
        float x, float y,
        int textColor
    ) {
        var matrix = context.getMatrices().peek().getPositionMatrix();
        var vertices = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
        var layer = net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    textRenderer.draw(
                        clean,
                        x + dx,
                        y + dy,
                        0xFF000000,
                        false,
                        matrix,
                        vertices,
                        layer,
                        0,
                        0xF000F0
                    );
                }
            }
        }

        textRenderer.draw(
            text,
            x,
            y,
            textColor,
            false,
            matrix,
            vertices,
            layer,
            0,
            0xF000F0
        );
    }    

    private OutlinedText() {}
}
