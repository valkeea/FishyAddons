package me.valkeea.fishyaddons.hud;

import java.awt.Rectangle;

import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class InfoDisplay implements HudElement {
    private InfoDisplay() {}
    private static InfoDisplay instance = null;
    public static InfoDisplay getInstance() {
        if (instance == null) {
            instance = new InfoDisplay();
        }
        return instance;
    }
    
    private boolean visible = false;
    private String message = "";
    private int hudX = 20;
    private int hudY = 40;
    private int width = 220;
    private int height = 60;
    private int bgColor = 0xAA222222;
    private int textColor = 0xFFFFFF;
    private boolean outlined = false;
    private boolean bg = true;

    public void register() {
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
            layeredDrawer.attachLayerAfter(
                IdentifiedLayer.MISC_OVERLAYS,
                Identifier.of("fishyaddons", "info_hud"),
                (context, tickCounter) -> render(context, 0, 0)
            )
        );
    }    

    public void show(String msg) {
        this.message = msg;
        this.visible = true;
    }

    public void hide() {
        this.visible = false;
        invalidateCache();
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!visible) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        String[] lines = message.replace("\\n", "\n").split("\n");
        int maxWidth = 0;
        int lineHeight = mc.textRenderer.fontHeight + 2;
        int hudHeight = 18 + (lines.length - 1) * lineHeight + 12;

        for (String line : lines) {
            int lineWidth = mc.textRenderer.getWidth(line);
            if (lineWidth > maxWidth) maxWidth = lineWidth;
        }

        context.fill(hudX, hudY, hudX + maxWidth + 24, hudY + hudHeight, bgColor);

        int textX = hudX + 12;
        int textY = hudY + 12;

        for (int i = 0; i < lines.length; i++) {
            context.drawText(mc.textRenderer, Text.literal(lines[i]), textX, textY + i * lineHeight, textColor, false);
        }

        int guideWidth = mc.textRenderer.getWidth("Press X to close");
        // fully opaque dark grey / black
        context.fill(hudX + 6, textY + lines.length * lineHeight + 6, hudX + guideWidth + 14, textY + lines.length * lineHeight + 18, 0xAA000000);
        context.drawText(mc.textRenderer, Text.literal("Press X to close"), textX, textY + lines.length * lineHeight + 8, 0xAAAAAA, false);
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
    @Override public void setEditingMode(boolean editing) { // not used
        }
    @Override public String getDisplayName() { return "Update Notification"; }
}