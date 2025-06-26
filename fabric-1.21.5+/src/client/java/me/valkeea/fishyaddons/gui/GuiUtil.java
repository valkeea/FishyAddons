package me.valkeea.fishyaddons.gui;

import net.minecraft.text.Text;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import java.util.List;

public class GuiUtil {
    public static void drawBox(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + height, 0x80000000);
        // Top
        context.fill(x - 1, y - 1, x + width + 1, y, color);
        // Bottom
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        // Left
        context.fill(x - 1, y, x, y + height, color);
        // Right
        context.fill(x + width, y, x + width + 1, y + height, color);
    }

    public static void fishyTooltip(DrawContext context, TextRenderer textRenderer, List<Text> lines, int mouseX, int mouseY) {
        if (lines == null || lines.isEmpty()) return;

        int tooltipX = mouseX + 10;
        int tooltipY = mouseY + 10;
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

        int height = lines.size() * 10 + 5;

        context.fill(tooltipX - 3, tooltipY - 3, tooltipX + width + 3, tooltipY + height + 3, 0x90000000);
        context.fill(tooltipX - 2, tooltipY - 2, tooltipX + width + 1, tooltipY + height + 1, 0xB0000000);

        for (int i = 0; i < lines.size(); i++) {
            context.drawTextWithShadow(textRenderer, lines.get(i), tooltipX, tooltipY + i * 10, 0xFFE2CAE9);
        }
        matrices.pop();
    }

    public static Text onOffLabel(String label, boolean enabled) {
        String state = enabled ? "ON" : "OFF";
        int color = enabled ? 0xCCFFCC : 0xFF8080;
        return Text.literal(label + ": ")
                .append(Text.literal(state).setStyle(Style.EMPTY.withColor(color)));
    } 

    // Replaced by texture overlays but may be added back later
    public static void lockedOverlay(DrawContext context, int x, int y) {
        int outerColor = 0x80E2CAE9;
        int innerColor = 0xC0E2CAE9;
        // Outer border (2px)
        context.fill(x, y, x + 16, y + 2, outerColor);
        context.fill(x, y + 14, x + 16, y + 16, outerColor);
        context.fill(x, y + 2, x + 2, y + 14, outerColor);
        context.fill(x + 14, y + 2, x + 16, y + 14, outerColor);
        // Inner fill
        context.fill(x + 2, y + 2, x + 14, y + 14, innerColor);
    }

    public static void boundOverlay(DrawContext context, int x, int y) {
        int outerColor = 0x80FFF6FA;
        int innerColor = 0xC0FFF6FA;
        context.fill(x, y, x + 16, y + 2, outerColor);
        context.fill(x, y + 14, x + 16, y + 16, outerColor);
        context.fill(x, y + 2, x + 2, y + 14, outerColor);
        context.fill(x + 14, y + 2, x + 16, y + 14, outerColor);
        context.fill(x + 2, y + 2, x + 14, y + 14, innerColor);
    }
}