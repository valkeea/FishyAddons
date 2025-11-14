package me.valkeea.fishyaddons.ui;

import java.util.List;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class GuiUtil {
    
    public static void drawBox(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + height, 0x80000000);
        context.fill(x - 1, y - 1, x + width + 1, y, color);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        context.fill(x - 1, y, x, y + height, color);
        context.fill(x + width, y, x + width + 1, y + height, color);
    }

    public static void wireRect(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x - 1, y - 1, x + width + 1, y, color);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        context.fill(x - 1, y, x, y + height, color);
        context.fill(x + width, y, x + width + 1, y + height, color);
    }
    
    public static void wireRect(DrawContext context, java.awt.Rectangle rect, int color) {
        wireRect(context, rect.x, rect.y, rect.width, rect.height, color);
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

        int height = lines.size() * 10 + 5;

        context.fill(tooltipX - 3, tooltipY - 3, tooltipX + width + 3, tooltipY + height + 3, 0x90000000);
        context.fill(tooltipX - 2, tooltipY - 2, tooltipX + width + 1, tooltipY + height + 1, 0xB0000000);

        for (int i = 0; i < lines.size(); i++) {
            context.drawTextWithShadow(textRenderer, lines.get(i), tooltipX, tooltipY + i * 10, 0xFFE2CAE9);
        }
    }

    public static Text onOffLabel(String label, boolean enabled) {
        String state = enabled ? "ON" : "OFF";
        int color = enabled ? 0xCCFFCC : 0xFF8080;
        return Text.literal(label + ": ")
                .append(Text.literal(state).setStyle(Style.EMPTY.withColor(color)));
    }

    public static MutableText onOffLabel(String label, boolean enabled, boolean drawsTitle) {
        String state = enabled ? "ON" : "OFF";
        int color = enabled ? 0xCCFFCC : 0xFF8080;
        if (drawsTitle) {
            return Text.literal(label + ": ")
                    .append(Text.literal(state).setStyle(Style.EMPTY.withColor(color)));
        }
        return Text.literal(state).setStyle(Style.EMPTY.withColor(color));
    }

    private GuiUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
}
