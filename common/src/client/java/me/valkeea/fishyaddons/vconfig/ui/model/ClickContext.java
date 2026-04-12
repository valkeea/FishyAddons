package me.valkeea.fishyaddons.vconfig.ui.model;

import net.minecraft.client.gui.Click;

public class ClickContext {
    public final Click click;
    public final int mouseX;
    public final int mouseY;
    public final int button;
    public final float uiScale;
    public final boolean doubled;
    
    public ClickContext(Click click, int mouseX, int mouseY, int button, float uiScale, boolean doubled) {
        this.click = click;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.button = button;
        this.uiScale = uiScale;
        this.doubled = doubled;
    }
    
    public static ClickContext fromClick(Click click, float uiScale, boolean doubled) {
        return new ClickContext(
            click,
            (int) click.x(),
            (int) click.y(),
            click.button(),
            uiScale,
            doubled
        );
    }
    
    public boolean isLeftClick() {
        return button == 0;
    }
}
