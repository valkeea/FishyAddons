package me.valkeea.fishyaddons.vconfig.ui.render;

import java.util.List;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class RenderUtils {
    private static final int TRIANGLE_OVERSAMPLE = 4;

    /** Simple tooltip */
    public static void preview(DrawContext context, TextRenderer textRenderer, List<Text> lines, int x, int y, int themeColor, float uiScale) {
        if (lines == null || lines.isEmpty()) return;

        int width = 0;
        for (Text line : lines) {
            int lineWidth = textRenderer.getWidth(line);
            if (lineWidth > width) {
                width = lineWidth;
            }
        }

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.scale(uiScale, uiScale);

        int tooltipX = (int)(x / uiScale) + 8;
        int tooltipY = (int)(y / uiScale);
        int height = lines.size() * 10 + 5;

        context.fill(tooltipX - 3, tooltipY - 3, tooltipX + width + 3, tooltipY + height + 3, 0x90000000);
        context.fill(tooltipX - 2, tooltipY - 2, tooltipX + width + 1, tooltipY + height + 1, 0xB0000000);

        for (int i = 0; i < lines.size(); i++) {
            context.drawText(textRenderer, lines.get(i), tooltipX, tooltipY + i * 10, themeColor, false);
        }
        matrices.popMatrix();
    }

    /** Orientation-aware gradient triangle with local oversampling for smoother edges. */
    public static void gradientTriangle(DrawContext context, int x, int y, int width, int height, int color, boolean isNorth) {
        if (width <= 0 || height <= 0) return;

        int oversample = Math.max(1, TRIANGLE_OVERSAMPLE);
        int scaledX = x * oversample;
        int scaledY = y * oversample;
        int scaledWidth = width * oversample;
        int scaledHeight = height * oversample;
        int centerX = scaledX + scaledWidth / 2;

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.scale(1.0f / oversample, 1.0f / oversample);

        for (int i = 0; i < scaledHeight; i++) {
            int lineY = isNorth ? scaledY + i : scaledY + scaledHeight - i - 1;
            int halfSpan = Math.min(i, scaledWidth / 2);
            int lineXStart = centerX - halfSpan;
            int lineXEnd = centerX + halfSpan + 1;
            int alpha = 255 - (255 * i / scaledHeight);
            int gradientColor = (color & 0x00FFFFFF) | (alpha << 24);
            context.fill(lineXStart, lineY, lineXEnd, lineY + 1, gradientColor);
        }

        matrices.popMatrix();
    }

    /** Simple border */
    public static void border(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    /** Rectangular, vertical gradient from slight alpha to transparent */
    public static void gradient(DrawContext context, int x, int y, int width, int height, int color) {
        int topColor = color | 0xF5000000;
        int bottomColor = color | 0x05000000;
        
        context.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
    }

    /** Rectangular, vertical opaque gradient  color -> black */
    public static void opaqueGradient(DrawContext context, int x, int y, int width, int height, int color) {
        int topColor = color | 0xFF000000;
        int bottomColor = 0xFF000000;
        
        context.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
    }
    
    public static void horizontalGradient(DrawContext context, int x, int y, int width, int height, int colorStart, int colorEnd) {
        if (width <= 0) return;
        
        int alphaStart = (colorStart >> 24) & 0xFF;
        int redStart = (colorStart >> 16) & 0xFF;
        int greenStart = (colorStart >> 8) & 0xFF;
        int blueStart = colorStart & 0xFF;
        
        int alphaEnd = (colorEnd >> 24) & 0xFF;
        int redEnd = (colorEnd >> 16) & 0xFF;
        int greenEnd = (colorEnd >> 8) & 0xFF;
        int blueEnd = colorEnd & 0xFF;
        
        for (int i = 0; i < width; i++) {
            float t = (float) i / (width - 1);
            
            int alpha = (int) (alphaStart + (alphaEnd - alphaStart) * t);
            int red = (int) (redStart + (redEnd - redStart) * t);
            int green = (int) (greenStart + (greenEnd - greenStart) * t);
            int blue = (int) (blueStart + (blueEnd - blueStart) * t);
            
            int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
            context.fill(x + i, y, x + i + 1, y + height, color);
        }
    }    

    private RenderUtils() {}    
}
