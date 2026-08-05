package me.valkeea.fishyaddons.hud.core;

import java.util.List;

import me.valkeea.fishyaddons.render.OutlinedText;
import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class HudDrawer {
    private Minecraft mc;
    private GuiGraphicsExtractor context;
    private HudElementState state;
    private boolean isShadow;

    public HudDrawer(Minecraft mc, GuiGraphicsExtractor context, HudElementState state) {
        this.mc = mc;
        this.context = context;
        this.state = state;
        this.isShadow = Config.get(me.valkeea.fishyaddons.vconfig.api.BooleanKey.HUD_TEXT_SHADOW);
    }

    /**
     * Text with color only
     */
    public void drawText(Component text, int x, int y, int color) {
        if (state.outlined) {
            OutlinedText.withColor(context, mc.font, text, x, y, color);
        } else {
            context.text(mc.font, text, x, y, color, isShadow);
        }
    }

    /**
     * Text with predefined formatting
     */
    public void drawFormattedText(Component text, int x, int y, int color) {
        if (state.outlined) {
            OutlinedText.withFormat(context, mc.font, text, x, y, color);
        } else {
            context.text(mc.font, text, x, y, color, isShadow);
        }
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        context.fill(x1, y1, x2, y2, color);
    }

    public void drawBorder(int x, int y, int width, int height, int color) {
        RenderUtils.border(context, x, y, width, height, color);
    }

    public void drawIcon(Identifier iconId, int x, int y, int width, int height) {
        context.blit(
            RenderPipelines.GUI_TEXTURED,
            iconId, x, y,
            0, 0, width, height,
            width, height
        );
    }

    public void drawIcon(Identifier iconId, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        context.blit(
            RenderPipelines.GUI_TEXTURED,
            iconId, x, y,
            u, v, width, height,
            textureWidth, textureHeight
        );
    }

    public void drawItem(net.minecraft.world.item.ItemStack stack, int x, int y) {
        context.item(stack, x, y);
    }

    public void drawButton(int x, int y, int width, int height, Component text, boolean hovered, boolean enabled) {
        int bgColor = VCVisuals.bgHex(hovered, enabled);
        int borderColor = VCVisuals.borderHex(hovered, enabled);

        RenderUtils.gradient(context, x, y, width, height, bgColor);
        RenderUtils.border(context, x, y, width, height, borderColor);

        drawText(text, x + (width - mc.font.width(text)) / 2, y + (height - 9) / 2, 0xFFFFFFFF);
    }

    /** Text button with centering and hover feedback */
    public void textButton(int x, int y, int width, int height, Component text, boolean hovered) {
        int textColor = hovered ? Color.brighten(state.color, 0.5f) : state.color;
        drawText(text, x + (width - mc.font.width(text)) / 2, y + (height - 9) / 2, textColor);
    }
    
    public void drawTooltip(GuiGraphicsExtractor context, List<Component> tooltip, int mouseX, int mouseY, int themeColor) {

        var screen = mc.getWindow();
        var tr = mc.font;
        int w = tr.width(tooltip.get(0)) + 10;
        int h = tooltip.size() * (tr.lineHeight + 2) + 4;

        if (mouseX + w > screen.getWidth() / screen.getGuiScale()) mouseX = screen.getWidth() / screen.getGuiScale() - w - 5;
        if (mouseY + h > screen.getHeight() / screen.getGuiScale()) mouseY = screen.getHeight() / screen.getGuiScale() - h - 5;

        RenderUtils.preview(context, mc.font, tooltip, mouseX, mouseY, themeColor, 1.0F);
    }

    public static boolean isShadow() {
        return Config.get(me.valkeea.fishyaddons.vconfig.api.BooleanKey.HUD_TEXT_SHADOW);
    }

    /** 
     * Static drawText for elements without state 
     */
    public static void drawText(GuiGraphicsExtractor ctx, Component text, int x, int y, int color, boolean outlined) {
        var tr = Minecraft.getInstance().font;
        if (outlined) {
            OutlinedText.withColor(ctx, tr, text, x, y, color);
        } else {
            ctx.text(tr, text, x, y, color, isShadow());
        }
    }
}
