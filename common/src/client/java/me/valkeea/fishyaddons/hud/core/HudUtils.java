package me.valkeea.fishyaddons.hud.core;

import java.util.List;

import me.valkeea.fishyaddons.ui.VCRenderUtils;
import me.valkeea.fishyaddons.ui.widget.VCVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class HudUtils {
    private HudUtils() {}
    
    /**
     * Calculate maximum width of text lines
     */
    public static int getMaxLineWidth(MinecraftClient mc, List<Text> lines, float scale) {
        int maxWidth = 0;
        for (Text line : lines) {
            int width = mc.textRenderer.getWidth(line.getString());
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return (int)(maxWidth * scale);
    }
    
    /**
     * Check if inventory screen is open
     */
    public static boolean isInventoryOpen(MinecraftClient mc) {
        if (mc.currentScreen == null) {
            return false;
        }
        
        String screenClassName = mc.currentScreen.getClass().getSimpleName();
        return screenClassName.equals("class_490") ||
               screenClassName.equals("class_476") ||
               screenClassName.equals("class_475") ||
               screenClassName.contains("Inventory");
    }
    
    /**
     * Standard background
     */
    public static void drawBackground(net.minecraft.client.gui.DrawContext context, 
                                     int x, int y, int width, int height) {
        context.fill(x + 1, y + 2, x + width + 2, y + height - 1, 0x80000000);
    }

    @SuppressWarnings("squid:S107")
    public static void iconButton(
        DrawContext ctx, int x, int y, int width, int height, boolean hovered, boolean enabled, Identifier icon
    ) {
        int bgColor = VCVisuals.bgHex(hovered, enabled);
        int borderColor = VCVisuals.borderHex(hovered, enabled);

        VCRenderUtils.gradient(ctx, x, y, width, height, bgColor);
        VCRenderUtils.border(ctx, x, y, width, height, borderColor);

        int iconX = x + (width - 16) / 2;
        int iconY = y + (height - 16) / 2;

        ctx.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            icon, iconX, iconY,
            0, 0, width, height,
            width, height
        );
    }    
    
    /**
     * Format large numbers with commas
     */
    public static String formatNum(long number) {
        return String.format("%,d", number);
    }
    
    /**
     * Format coins/value display
     */
    public static String formatCoins(double coins) {
        if (coins >= 1_000_000.0) {
            return String.format("%.1fM", coins / 1_000_000.0);
        } else if (coins >= 1_000.0) {
            return String.format("%.1fK", coins / 1_000.0);
        } else {
            return String.format("%.0f", coins);
        }
    }
    
    /**
     * Calculate scaled dimensions
     */
    public static int[] getScaledDimensions(int baseSize, int lineCount, int maxWidth, float scale) {
        int scaledWidth = (int)(maxWidth * scale);
        int scaledHeight = (int)(lineCount * baseSize * scale);
        return new int[]{scaledWidth, scaledHeight};
    }
}
