package me.valkeea.fishyaddons.ui;

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
            int gradientColor = (color & 0x00FFFFFF) | (255 * 0x1000000);
            context.fill(x, y + i, x + width, y + i + 1, gradientColor);
        }
    }

    private VCRenderUtils() {
        throw new UnsupportedOperationException("Utility class");
    }    
}
