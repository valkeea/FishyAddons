package me.valkeea.fishyaddons.vconfig.ui.model;

import net.minecraft.client.gui.Click;

public class DragContext {
    public final Click click;
    public final int mouseX;
    public final int mouseY;
    public final double offsetX;
    public final double offsetY;
    public final int button;
    public final float uiScale;
    
    public DragContext(Click click, int mouseX, int mouseY, double offsetX, double offsetY, float uiScale) {
        this.click = click;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.button = click.button();
        this.uiScale = uiScale;
    }

    public static DragContext fromDrag(Click click, double offsetX, double offsetY, float uiScale) {
        return new DragContext(
            click,
            (int) click.x(),
            (int) click.y(),
            offsetX,
            offsetY,
            uiScale
        );
    }
}
