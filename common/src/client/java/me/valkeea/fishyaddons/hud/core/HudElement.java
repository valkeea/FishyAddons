package me.valkeea.fishyaddons.hud.core;

import java.awt.Rectangle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

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
    void render(GuiGraphicsExtractor context, Minecraft mc, int mouseX, int mouseY);
    String getDisplayName();
    Rectangle getBounds(Minecraft mc);
    HudElementState getCachedState();
    void invalidateCache();
    
    default boolean isConfigurable() {
        return true;
    }

    default boolean hasCosmetics() {
        return true;
    }

    default void resetAll() {
        if (isConfigurable()) {
            setHudPosition(5, 5);
            setHudSize(12);
            setHudColor(0xFFFFFFFF);
            setHudOutline(false);
            setHudBg(false);
            invalidateCache();
        }
    }    
}
