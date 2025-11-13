package me.valkeea.fishyaddons.hud.core;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.render.OutlinedText;
import me.valkeea.fishyaddons.ui.VCRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Helper class for drawing HUD texts with proper visuals (shadow/outline) based on HudElementState and config
 */
public class HudDrawer {
    private MinecraftClient mc;
    private DrawContext context;
    private HudElementState state;
    private boolean isShadow;

    public HudDrawer(MinecraftClient mc, DrawContext context, HudElementState state) {
        this.mc = mc;
        this.context = context;
        this.state = state;
        this.isShadow = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.HUD_TEXT_SHADOW, true);
    }

    public static boolean isShadow() {
        return FishyConfig.getState(me.valkeea.fishyaddons.config.Key.HUD_TEXT_SHADOW, true);
    }

    /**
     * Text with color only
     */
    public void drawText(Text text, int x, int y, int color) {
        if (state.outlined) {
            OutlinedText.withColor(context, mc.textRenderer, text, x, y, color);
        } else {
            context.drawText(mc.textRenderer, text, x, y, color, isShadow);
        }
    }

    /**
     * Text with predefined formatting
     */
    public void drawFormattedText(Text text, int x, int y, int color) {
        if (state.outlined) {
            OutlinedText.withFormat(context, mc.textRenderer, text, x, y, color);
        } else {
            context.drawText(mc.textRenderer, text, x, y, color, isShadow);
        }
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y2, color);
    }

    public void drawBorder(int x, int y, int width, int height, int color) {
        VCRenderUtils.border(context, x, y, width, height, color);
    }
    
    /**
     * Static method for elements without state
     */
    public static void drawText(DrawContext ctx, Text text, int x, int y, int color, boolean outlined) {
        var tr = MinecraftClient.getInstance().textRenderer;
        if (outlined) {
            OutlinedText.withColor(ctx, tr, text, x, y, color);
        } else {
            ctx.drawText(tr, text, x, y, color, isShadow());
        }
    }
}
