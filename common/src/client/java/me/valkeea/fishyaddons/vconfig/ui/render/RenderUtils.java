package me.valkeea.fishyaddons.vconfig.ui.render;

import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class RenderUtils {
    private static final int TRIANGLE_OVERSAMPLE = 4;

    /** 
     * Convenience method to create a VCTooltip and render it.
     */
    public static void preview(GuiGraphicsExtractor gge, Font tr, List<Component> lines, int x, int y, int color, float scale) {
        if (lines == null || lines.isEmpty()) return;
        var tt = new VCTooltip(lines, color, scale);
        tt.render(gge, tr, x, y);
    }

    /** Orientation-aware gradient triangle with local oversampling for smoother edges. */
    public static void gradientTriangle(GuiGraphicsExtractor gge, int x, int y, int w, int h, int color, boolean isNorth) {
        if (w <= 0 || h <= 0) return;

        int oversample = Math.max(1, TRIANGLE_OVERSAMPLE);
        int scaledX = x * oversample;
        int scaledY = y * oversample;
        int scaledW = w * oversample;
        int scaledH = h * oversample;
        int centerX = scaledX + scaledW / 2;

        var matrices = gge.pose();
        matrices.pushMatrix();
        matrices.scale(1.0f / oversample, 1.0f / oversample);

        for (int i = 0; i < scaledH; i++) {
            int lineY = isNorth ? scaledY + i : scaledY + scaledH - i - 1;
            int halfSpan = Math.min(i, scaledW / 2);
            int lineXStart = centerX - halfSpan;
            int lineXEnd = centerX + halfSpan + 1;
            int alpha = 255 - (255 * i / scaledH);
            int gradientColor = (color & 0x00FFFFFF) | (alpha << 24);
            gge.fill(lineXStart, lineY, lineXEnd, lineY + 1, gradientColor);
        }

        matrices.popMatrix();
    }

    /** Simple border */
    public static void border(GuiGraphicsExtractor gge, int x, int y, int w, int h, int color) {
        gge.fill(x, y, x + w, y + 1, color);
        gge.fill(x, y + h - 1, x + w, y + h, color);
        gge.fill(x, y + 1, x + 1, y + h - 1, color);
        gge.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Rectangular, vertical gradient from slight alpha to transparent */
    public static void gradient(GuiGraphicsExtractor gge, int x, int y, int w, int h, int color) {
        int topColor = color | 0xF5000000;
        int bottomColor = color | 0x05000000;
        
        gge.fillGradient(x, y, x + w, y + h, topColor, bottomColor);
    }

    /** Rectangular, vertical opaque gradient  color -> black */
    public static void opaqueGradient(GuiGraphicsExtractor gge, int x, int y, int w, int h, int color) {
        int topColor = color | 0xFF000000;
        int bottomColor = 0xFF000000;
        
        gge.fillGradient(x, y, x + w, y + h, topColor, bottomColor);
    }
    
    public static void horizontalGradient(GuiGraphicsExtractor gge, int x, int y, int w, int h, int colorStart, int colorEnd) {
        if (w <= 0) return;
        
        int alphaStart = (colorStart >> 24) & 0xFF;
        int redStart = (colorStart >> 16) & 0xFF;
        int greenStart = (colorStart >> 8) & 0xFF;
        int blueStart = colorStart & 0xFF;
        
        int alphaEnd = (colorEnd >> 24) & 0xFF;
        int redEnd = (colorEnd >> 16) & 0xFF;
        int greenEnd = (colorEnd >> 8) & 0xFF;
        int blueEnd = colorEnd & 0xFF;
        
        for (int i = 0; i < w; i++) {
            float t = (float) i / (w - 1);
            
            int alpha = (int) (alphaStart + (alphaEnd - alphaStart) * t);
            int red = (int) (redStart + (redEnd - redStart) * t);
            int green = (int) (greenStart + (greenEnd - greenStart) * t);
            int blue = (int) (blueStart + (blueEnd - blueStart) * t);
            
            int color = (alpha << 24) | (red << 16) | (green << 8) | blue;
            gge.fill(x + i, y, x + i + 1, y + h, color);
        }
    }

    /** Framed rectangle with a vertical gradient as inner fill */
    public static void frame(GuiGraphicsExtractor gge, int x, int y, int w, int h, int colorStart, int colorEnd) {
        gge.horizontalLine(x + 1, x + w - 2, y, colorStart);
        gge.horizontalLine(x + 1, x + w - 2, y + h, colorEnd);
        gge.fillGradient(x, y, x + 1, y + h, colorStart, colorEnd);
        gge.fillGradient(x + w - 1, y, x + w, y + h, colorStart, colorEnd);
        gge.fillGradient(x + 3, y + 3, x + w - 3, y + h - 3, colorStart, colorEnd);
    }

    private RenderUtils() {}    
}
