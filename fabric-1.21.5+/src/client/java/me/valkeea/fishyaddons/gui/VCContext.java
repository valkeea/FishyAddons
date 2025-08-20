package me.valkeea.fishyaddons.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Parameter object to group rendering context
 */
public class VCContext {
    public final DrawContext context;
    public final int mouseX;
    public final int mouseY;
    public final int entryX;
    public final int entryWidth;

    public VCContext(DrawContext context, int mouseX, int mouseY, int entryX, int entryWidth) {
        this.context = context;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.entryX = entryX;
        this.entryWidth = entryWidth;
    }
}
