package me.valkeea.fishyaddons.gui;

import java.util.List;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class VCRenderUtils {

    // Tooltip for expandable
    public static void preview(DrawContext context, TextRenderer textRenderer, List<Text> lines, int x, int y, int themeColor, float uiScale) {
        if (lines == null || lines.isEmpty()) return;

        // Calculate width once
        int width = 0;
        for (Text line : lines) {
            int lineWidth = textRenderer.getWidth(line);
            if (lineWidth > width) {
                width = lineWidth;
            }
        }

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(0, 0, 400);
        matrices.scale(uiScale, uiScale, 1);

        // Apply scaling to mouse coordinates and add slight offset to the right
        int tooltipX = (int)(x / uiScale) + 8;
        int tooltipY = (int)(y / uiScale);
        int height = lines.size() * 10 + 5;

        context.fill(tooltipX - 3, tooltipY - 3, tooltipX + width + 3, tooltipY + height + 3, 0x90000000);
        context.fill(tooltipX - 2, tooltipY - 2, tooltipX + width + 1, tooltipY + height + 1, 0xB0000000);

        for (int i = 0; i < lines.size(); i++) {
            context.drawTextWithShadow(textRenderer, lines.get(i), tooltipX, tooltipY + i * 10, themeColor);
        }
        matrices.pop();
    }

    // Orientation-aware gradient triangle
    public static void gradientTriangle(DrawContext context, int x, int y, int width, int height, int color, boolean isNorth) {
        for (int i = 0; i < height; i++) {
            int lineY = isNorth ? y + i : y + height - i - 1;
            int lineXStart = x + (width / 2) - i;
            int lineXEnd = x + (width / 2) + i;
            int gradientColor = (color & 0x00FFFFFF) | ((255 - (255 * i / height)) * 0x1000000);
            context.fill(lineXStart, lineY, lineXEnd, lineY + 1, gradientColor);
        }
    }

    // NW - Top Left
    public static void drawNw(DrawContext context, int x, int y, int width, int height, int color) {
        for (int i = 0; i < 2; i++) {
            int lineY = y + i;
            int gradientColor = (color & 0x00FFFFFF) | ((255 - (255 * i / 2)) * 0x1000000);
            context.fill(x, lineY, x + width, lineY + 1, gradientColor);
        }
        for (int i = 0; i < 2; i++) {
            int lineX = x + i;
            int gradientColor = (color & 0x00FFFFFF) | ((255 - (255 * i / 2)) * 0x1000000);
            context.fill(lineX, y, lineX + 1, y + height, gradientColor);
        }
    }

    // SE - Bottom Right
    public static void drawSe(DrawContext context, int x, int y, int width, int height, int color) {
        for (int i = 0; i < 2; i++) {
            int lineY = y + height - i;
            int gradientColor = (color & 0xFFFFFFFF) | ((255 - (255 * i / 2)) * 0x1000000);
            context.fill(x, lineY, x + width, lineY + 1, gradientColor);
        }
        for (int i = 0; i < 2; i++) {
            int lineX = x + width - i;
            int gradientColor = (color & 0xFFFFFFFF) | ((255 - (255 * i / 2)) * 0x1000000);
            context.fill(lineX, y, lineX + 1, y + height, gradientColor);
        }
    }

    public static void draw3dButton(DrawContext context, int x, int y, int width, int height, int color) {
        drawNw(context, x, y, width, height, color);
        drawSe(context, x, y, width, height, color);
    }

    public static void border(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    // Rectangular, vertical gradient fills
    public static void gradient(DrawContext context, int x, int y, int width, int height, int color) {
        for (int i = 0; i < height; i++) {
            int gradientColor = (color & 0x00FFFFFF) | ((255 - (255 * i / height)) * 0x1000000);
            context.fill(x, y + i, x + width, y + i + 1, gradientColor);
        }
    }

    public static void opaqueGradient(DrawContext context, int x, int y, int width, int height, int color) {
        for (int i = 0; i < height; i++) {
            context.fill(x, y + i, x + width, y + i + 1, color);
        }
    }

    private VCRenderUtils() {
        throw new UnsupportedOperationException("Utility class");
    }    
}
