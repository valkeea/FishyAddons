package me.valkeea.fishyaddons.hud.core;

import java.awt.Rectangle;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public interface HudElement {
    int getHudX();
    int getHudY();
    void setHudPosition(int x, int y);
    int getHudSize();
    void setHudSize(int size);
    int getHudColor();
    void setHudColor(int color);
    boolean getHudOutline();
    void setHudOutline(boolean outline);
    boolean getHudBg();
    void setHudBg(boolean bg);
    void setEditingMode(boolean editing);
    void render(DrawContext context, int mouseX, int mouseY);
    String getDisplayName();
    Rectangle getBounds(MinecraftClient mc);
    HudElementState getCachedState();
    void invalidateCache();
    
    default boolean isConfigurable() {
        return true;
    }
}
