package me.valkeea.fishyaddons.vconfig.ui.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class VCRenderContext {
    public final DrawContext context;
    public final TextRenderer textRenderer;
    public final int mouseX;
    public final int mouseY;
    public final float delta;
    public final int themeColor;
    public final int entryWidth;
    
    public VCRenderContext(
        DrawContext context,
        TextRenderer textRenderer,
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
