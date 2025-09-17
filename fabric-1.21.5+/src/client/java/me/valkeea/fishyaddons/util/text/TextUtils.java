package me.valkeea.fishyaddons.util.text;

import java.util.List;

import org.joml.Matrix4f;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

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

        Style origStyle = copied.getStyle() != null ? copied.getStyle() : Style.EMPTY;
        boolean isMaxLevel = copied.getString().equals("MAX LEVEL");
        Style newStyle = Style.EMPTY.withColor(0x000000);

        if (isMaxLevel && origStyle.isBold()) {
            newStyle = newStyle.withBold(true);
        }

        if (copied instanceof MutableText mutableText) {
            mutableText.setStyle(newStyle);
        }

        List<Text> recoloredSiblings = copied.getSiblings().stream()
            .map(TextUtils::recolor)
            .toList();

        copied.getSiblings().clear();
        if (copied instanceof MutableText mutableText) {
            recoloredSiblings.forEach(mutableText::append);
        }

        return copied;
    }
}

