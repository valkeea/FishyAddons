package me.valkeea.fishyaddons.hud.core;

import java.util.List;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.render.OutlinedText;
import me.valkeea.fishyaddons.ui.VCRenderUtils;
import me.valkeea.fishyaddons.ui.widget.VCVisuals;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

    public void drawIcon(Identifier iconId, int x, int y, int width, int height) {
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            iconId, x, y,
            0, 0, width, height,
            width, height
        );
    }

    public void drawItem(net.minecraft.item.ItemStack stack, int x, int y) {
        context.drawItem(stack, x, y);
    }

    public void drawButton(int x, int y, int width, int height, Text text, boolean hovered, boolean enabled) {
        int bgColor = VCVisuals.bgHex(hovered, enabled);
        int borderColor = VCVisuals.borderHex(hovered, enabled);

        VCRenderUtils.gradient(context, x, y, width, height, bgColor);
        VCRenderUtils.border(context, x, y, width, height, borderColor);

        drawText(text, x + (width - mc.textRenderer.getWidth(text)) / 2, y + (height - 9) / 2, 0xFFFFFFFF);
    }
    
    public void drawTooltip(DrawContext context, List<Text> tooltip, int mouseX, int mouseY, int themeColor) {
        VCRenderUtils.preview(context, mc.textRenderer, tooltip, mouseX, mouseY, themeColor, 1.0F);
    }

    public static boolean isShadow() {
        return FishyConfig.getState(me.valkeea.fishyaddons.config.Key.HUD_TEXT_SHADOW, true);
    }

    /** 
     * Static drawText for elements without state 
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
