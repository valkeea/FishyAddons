package me.valkeea.fishyaddons.util;

import java.util.List;

import org.joml.Matrix4f;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;

public class TextUtils {
    private TextUtils() {}
    
    public static void drawOutlinedText(
        DrawContext context,
        TextRenderer textRenderer,
        Text text,
        float x, float y,
        int textColor,
        int outlineColor,
        Matrix4f matrix,
        VertexConsumerProvider vertexConsumers,
        TextRenderer.TextLayerType layerType,
        int light
    ) {
        // Draw text offset in 8 directions
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) {
                    textRenderer.draw(
                        text,
                        x + dx,
                        y + dy,
                        outlineColor,
                        false,
                        matrix,
                        vertexConsumers,
                        layerType,
                        0,
                        light
                    );
                }
            }
        }

        // Foreground
        textRenderer.draw(
            text,
            x,
            y,
            textColor,
            false,
            matrix,
            vertexConsumers,
            layerType,
            0,
            light
        );
    }

    public static Text stripColor(Text text) {
        MutableText base = Text.literal(text.getString());
        for (Text sibling : text.getSiblings()) {
            base.append(stripColor(sibling));
        }
        return base;
    }  

    public static Text recolor(Text original) {
        Text copied = original.copy();

        // Modify style to black, preserve bold
        Style origStyle = copied.getStyle() != null ? copied.getStyle() : Style.EMPTY;
        boolean isMaxLevel = copied.getString().equals("MAX LEVEL");
        Style newStyle = Style.EMPTY.withColor(0x000000);

        if (isMaxLevel && origStyle.isBold()) {
            newStyle = newStyle.withBold(true);
        }

        if (copied instanceof MutableText mutableText) {
            mutableText.setStyle(newStyle);
        }

        // Recursively recolor siblings
        List<Text> recoloredSiblings = copied.getSiblings().stream()
            .map(TextUtils::recolor)
            .toList();

        // Clear existing siblings and add recolored ones
        copied.getSiblings().clear();
        if (copied instanceof MutableText mutableText) {
            recoloredSiblings.forEach(mutableText::append);
        }

        return copied;
    }
}

