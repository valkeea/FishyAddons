package me.valkeea.fishyaddons.vconfig.ui.model;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class BaseContext {
    public final GuiGraphicsExtractor context;
    public final int mouseX;
    public final int mouseY;
    public final int entryX;
    public final int entryWidth;

    public BaseContext(GuiGraphicsExtractor context, int mouseX, int mouseY, int entryX, int entryWidth) {
        this.context = context;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.entryX = entryX;
        this.entryWidth = entryWidth;
    }
}
