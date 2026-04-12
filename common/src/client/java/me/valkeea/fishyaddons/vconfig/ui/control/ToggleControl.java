package me.valkeea.fishyaddons.vconfig.ui.control;

import me.valkeea.fishyaddons.vconfig.binding.ConfigBinding.BooleanBinding;
import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.model.Bounds;
import me.valkeea.fishyaddons.vconfig.ui.model.ClickContext;
import me.valkeea.fishyaddons.vconfig.ui.render.VCRenderContext;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCButton;

public class ToggleControl extends AbstractUIControl {
    
    private final BooleanBinding binding;
    private final String label;
    private boolean cachedValue;
    private String buttonText;
    private int widthCache = Dimensions.BUTTON_W;    
    
    public ToggleControl(BooleanBinding binding, String[] tooltip) {
        this(binding, "", tooltip);
    }

    public ToggleControl(BooleanBinding binding, String label, String[] tooltip) {
        this.binding = binding;
        this.label = label;
        this.tooltip = tooltip;
    }

    @Override
    public void render(VCRenderContext ctx, int x, int y) { 
        if (cachedValueDirty) {
            cachedValue = binding.get();
            buttonText = getButtonText();
            widthCache = calculateWidth(ctx);
            markCacheFresh();
        }    

        widthCache = calculateWidth(ctx);
        int h = Dimensions.BUTTON_H;
        
        this.lastBounds = new Bounds(x, y, widthCache, h);

        VCButton.render(
            ctx.context,
            ctx.textRenderer,
            VCButton.toggleWithText(x, y, widthCache, h, buttonText, cachedValue)
                .withHovered(hovered)
        );
    }

    @Override
    public int getPreferredWidth() {
        return widthCache;
    }

    @Override
    public boolean handleClick(ClickContext ctx, int x, int y) {
        if (lastBounds == null || !ctx.isLeftClick() ||
            !lastBounds.contains(ctx.mouseX, ctx.mouseY)) return false;
        cachedValue = !cachedValue;
        binding.set(cachedValue);
        buttonText = getButtonText();
        return true;
    }

    private int calculateWidth(VCRenderContext ctx) {
        return Dimensions.getCustomButtonW(
            VCText.getWidthWithPadding(
                ctx.textRenderer,
                getButtonText()
            )
        );
    }    

    private String getButtonText() {
        if (!label.isEmpty()) return label;
        return cachedValue ? "ON" : "OFF";
    }
}
