package me.valkeea.fishyaddons.vconfig.ui.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class VCRenderContext {
    public final GuiGraphicsExtractor context;
    public final Font textRenderer;
    public final int mouseX;
    public final int mouseY;
    public final float delta;
    public final int themeColor;
    public final int entryWidth;
    
    public VCRenderContext(
        GuiGraphicsExtractor context,
        Font textRenderer,
        int mouseX,
        int mouseY,
        float delta,
        int themeColor,
        int entryWidth
    ) {
        this.context = context;
        this.textRenderer = textRenderer;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.delta = delta;
        this.themeColor = themeColor;
        this.entryWidth = entryWidth;
    }
}
