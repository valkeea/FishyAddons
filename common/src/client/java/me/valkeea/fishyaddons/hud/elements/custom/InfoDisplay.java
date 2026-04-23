package me.valkeea.fishyaddons.hud.elements.custom;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.util.ModInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class InfoDisplay implements HudElement {
    private InfoDisplay() {}
    private static final boolean LINK_BTN = true;    
    private static InfoDisplay instance = null;

    public static InfoDisplay getInstance() {
        if (instance == null) {
            instance = new InfoDisplay();
        }
        return instance;
    }

    public static boolean isVisible() {
        return instance != null && instance.visible;
    }    

    private boolean visible = false;
    private String message = "";
    private int hudX = 20;
    private int hudY = 40;
    private int width = 220;
    private int height = 60;
    private int textColor = 0xFFFFFFFF;
    private boolean outlined = false;
    private boolean bg = true; 

    public void show(String msg) {
        if (msg != null && !msg.trim().isEmpty()) {
            this.message = msg;
            this.visible = true;
        }
    }

    public void hide() {
        this.visible = false;
        this.message = "";
        invalidateCache();
    }

    public void forceHide() {
        hide();
    }

    public void externalDisplay(DrawContext context, TextRenderer tr, String msg) {
        draw(context, tr, msg, true);
    }

    @Override
    public void render(DrawContext ctx, MinecraftClient mc, int mouseX, int mouseY) {
        if (!visible || !ModInfo.shouldShowInfo() || message == null || message.trim().isEmpty()) {
            if (visible && !ModInfo.shouldShowInfo()) {
                forceHide();
            }
            return;
        }
        draw(ctx, mc.textRenderer, message, false);
    }

    private void draw(DrawContext ctx, TextRenderer tr, String msg, boolean external) {
        if (tr == null) return;

        String[] lines = msg.replace("\\n", "\n").split("\n");
        int maxWidth = 0;
        int lineHeight = tr.fontHeight + 2;
        int hudHeight = 18 + (lines.length - 1) * lineHeight + 12;

        for (String line : lines) {
            int lineWidth = tr.getWidth(line);
            if (lineWidth > maxWidth) maxWidth = lineWidth;
        }

        int bgColor = external ? 0xFF111111 : 0xAA111111;
        ctx.fill(hudX, hudY, hudX + maxWidth + 24, hudY + hudHeight, bgColor);

        int textX = hudX + 12;
        int textY = hudY + 12;

        for (int i = 0; i < lines.length; i++) {
            ctx.drawText(tr, Text.literal(lines[i]), textX, textY + i * lineHeight, textColor, false);
        }

        if (external) return;

        int guideW = tr.getWidth("Press X to close");
        int btnY = textY + lines.length * lineHeight + 6;
        ctx.fill(hudX + 6, btnY, hudX + guideW + 14, btnY + 12, 0xAA000000);
        ctx.drawText(tr, Text.literal("Press X to close"), textX, btnY + 2, 0xFFAAAAAA, false);

        if (!LINK_BTN) return;
        
        int copyW = tr.getWidth("C to copy link");
        int copyX = hudX + 18 + guideW;
        ctx.fill(copyX, btnY, hudX + guideW + copyW + 28, btnY + 12, 0xAA000000);
        ctx.drawText(tr, Text.literal("C to copy link"), copyX + 7, btnY + 2, 0xFFAAAAAA, false);
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {
        return new Rectangle(hudX, hudY, width, height);
    }

    @Override
    public HudElementState getCachedState() {
        return new HudElementState(hudX, hudY, height, textColor, outlined, bg);
    }

    @Override
    public void invalidateCache() {
        // No cached state to invalidate
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }    

    @Override public int getHudX() { return hudX; }
    @Override public int getHudY() { return hudY; }
    @Override public void setHudPosition(int x, int y) { this.hudX = x; this.hudY = y; }
    @Override public int getHudSize() { return height; }
    @Override public void setHudSize(int size) { this.height = size; }
    @Override public int getHudColor() { return textColor; }
    @Override public void setHudColor(int color) { this.textColor = color; }
    @Override public boolean getHudOutline() { return outlined; }
    @Override public void setHudOutline(boolean outline) { this.outlined = outline; }
    @Override public boolean getHudBg() { return bg; }
    @Override public void setHudBg(boolean bg) { this.bg = bg; }
    @Override public void setEditingMode(boolean editing) { /** not used */ }
    @Override public String getDisplayName() { return "Update Notification"; }
}
