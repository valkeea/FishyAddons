package me.valkeea.fishyaddons.vconfig.ui.control;

import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.model.Bounds;
import me.valkeea.fishyaddons.vconfig.ui.model.ClickContext;
import me.valkeea.fishyaddons.vconfig.ui.render.VCRenderContext;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCButton;

public class ButtonControl extends AbstractUIControl {
    
    private final String buttonText;
    private final Runnable action;

    private int widthCache = Dimensions.BUTTON_W;
    
    public ButtonControl(String buttonText, Runnable action, String[] tooltip) {
        this.buttonText = buttonText;
        this.action = action;
        this.tooltip = tooltip;
    }
    
    @Override
    public void render(VCRenderContext ctx, int x, int y) {
        calculateWidth(ctx);
        int h = Dimensions.BUTTON_H;
        
        this.lastBounds = new Bounds(x, y, widthCache, h);
        
        VCButton.render(
            ctx.context,
            ctx.textRenderer,
            new VCButton.ButtonConfig(x, y, widthCache, h, buttonText)
                .withHovered(hovered)
        );
    }
    
    @Override
    public boolean handleClick(ClickContext ctx, int x, int y) {
        if (lastBounds == null || !ctx.isLeftClick() ||
            !lastBounds.contains(ctx.mouseX, ctx.mouseY)) {
            return false;
        }
        
        if (action != null) {
            ScreenManager.preserveCurrentState();
            action.run();
        }
        
        return true;
    }

    private void calculateWidth(VCRenderContext ctx) {
        widthCache = Dimensions.getCustomButtonW(
            VCText.getWidthWithPadding(
                ctx.textRenderer,
                buttonText
            )
        );
    }

    @Override
    public int getPreferredWidth() {
        return widthCache;
    }
}
