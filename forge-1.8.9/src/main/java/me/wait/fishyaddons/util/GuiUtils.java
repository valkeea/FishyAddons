package me.wait.fishyaddons.util;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;

import java.util.List;


public class GuiUtils {

    private GuiUtils() {}

    // Custom tooltip for FishyAddons GUIs
    public static void drawTooltip(List<String> tooltip, int mouseX, int mouseY, FontRenderer fontRenderer) {
        if (tooltip == null || tooltip.isEmpty()) return;

        int tooltipX = mouseX + 10;
        int tooltipY = mouseY + 10;
        int width = 0;

        for (String line : tooltip) {
            int lineWidth = fontRenderer.getStringWidth(line);
            if (lineWidth > width) {
                width = lineWidth;
            }
        }

        int height = tooltip.size() * 10 + 5;

        GuiScreen.drawRect(tooltipX - 3, tooltipY - 3, tooltipX + width + 3, tooltipY + height + 3, 0x90000000);

        for (int i = 0; i < tooltip.size(); i++) {
            fontRenderer.drawString(tooltip.get(i), tooltipX, tooltipY + i * 10, 0xFFE2CAE9);
        }
    }
}