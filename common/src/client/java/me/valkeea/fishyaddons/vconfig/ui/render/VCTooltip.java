package me.valkeea.fishyaddons.vconfig.ui.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class VCTooltip {
    private static final int PADDING = 3;
    private static final int LINE_HEIGHT = 10;
    
    private final List<Text> lines;
    private final int themeColor;
    private final float uiScale;
    
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private TextRenderer cachedRenderer = null;

    public VCTooltip(List<Text> lines, int themeColor, float uiScale) {
        this.lines = new ArrayList<>(lines);
        this.themeColor = themeColor;
        this.uiScale = uiScale;
    }

    public VCTooltip(int themeColor, float uiScale, Text... lines) {
        this(Arrays.asList(lines), themeColor, uiScale);
    }

    /**
     * Get the scaled width of the tooltip.
     */
    public int getScaledWidth(TextRenderer textRenderer) {
        calculateDimensions(textRenderer);
        return (int)((cachedWidth + PADDING * 2) * uiScale);
    }

    /**
     * Get the scaled height of the tooltip.
     */
    public int getScaledHeight(TextRenderer textRenderer) {
        calculateDimensions(textRenderer);
        return (int)((cachedHeight + PADDING * 2) * uiScale);
    }

    /**
     * Get the unscaled content width (before UI scaling).
     */
    public int getContentWidth(TextRenderer textRenderer) {
        calculateDimensions(textRenderer);
        return cachedWidth;
    }

    /**
     * Get the unscaled content height (before UI scaling).
     */
    public int getContentHeight(TextRenderer textRenderer) {
        calculateDimensions(textRenderer);
        return cachedHeight;
    }

    private void calculateDimensions(TextRenderer textRenderer) {
        if (cachedWidth != -1 && cachedRenderer == textRenderer) {
            return;
        }

        cachedRenderer = textRenderer;
        cachedWidth = 0;
        for (Text line : lines) {
            int lineWidth = textRenderer.getWidth(line);
            if (lineWidth > cachedWidth) {
                cachedWidth = lineWidth;
            }
        }
        cachedHeight = lines.size() * LINE_HEIGHT + 5;
    }

    /**
     * Calculate optimal tooltip position based on target position and screen bounds.
     * Returns adjusted position as [x, y].
     */
    public int[] calcPosition(TextRenderer textRenderer, int targetX, int targetY, int screenWidth, int screenHeight) {
        int scaledWidth = getScaledWidth(textRenderer);
        int scaledHeight = getScaledHeight(textRenderer);
        
        int x = targetX + 8;
        int y = targetY;
        int hOf = x + scaledWidth - screenWidth;
        int vOf = y + scaledHeight - screenHeight;

        if (hOf > 0) {
            x = targetX - hOf - 8;
            if (x < 0) {
                x = screenWidth - scaledWidth - 5;
            }
        }

        if (vOf > 0) {
            y = targetY - vOf;
            if (y < 0) {
                y = 5;
            }
        }

        return new int[]{x, y};
    }

    /**
     * Render the tooltip at the specified position.
     */
    public void render(DrawContext context, TextRenderer textRenderer, int x, int y) {
        if (lines == null || lines.isEmpty()) return;

        calculateDimensions(textRenderer);

        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.scale(uiScale, uiScale);

        int tooltipX = (int)(x / uiScale) + 8;
        int tooltipY = (int)(y / uiScale);
        int width = cachedWidth;
        int height = cachedHeight;

        context.fill(tooltipX - PADDING, tooltipY - PADDING, 
                     tooltipX + width + PADDING, tooltipY + height + PADDING, 0x90000000);
        context.fill(tooltipX - PADDING + 1, tooltipY - PADDING + 1, 
                     tooltipX + width + PADDING - 2, tooltipY + height + PADDING - 2, 0xB0000000);

        for (int i = 0; i < lines.size(); i++) {
            context.drawText(textRenderer, lines.get(i), tooltipX, tooltipY + i * LINE_HEIGHT, themeColor, false);
        }
        
        matrices.popMatrix();
    }

    /**
     * Render the tooltip with automatic positioning based on screen bounds.
     */
    public void renderAuto(DrawContext context, TextRenderer textRenderer, int targetX, int targetY, int screenWidth, int screenHeight) {
        int[] pos = calcPosition(textRenderer, targetX, targetY, screenWidth, screenHeight);
        render(context, textRenderer, pos[0], pos[1]);
    }
}
