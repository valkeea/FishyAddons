package me.valkeea.fishyaddons.hud;

import net.minecraft.client.gui.DrawContext;

public interface HudElement {
    int getHudX();
    int getHudY();
    void setHudPosition(int x, int y);
    int getHudSize();
    void setHudSize(int size);
    int getHudColor();
    void setHudColor(int color);
    void setEditingMode(boolean editing);
    void render(DrawContext context, int mouseX, int mouseY);
    String getDisplayName();
}