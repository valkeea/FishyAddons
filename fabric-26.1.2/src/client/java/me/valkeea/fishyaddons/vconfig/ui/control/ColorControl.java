package me.valkeea.fishyaddons.vconfig.ui.control;

import java.util.function.Consumer;

import me.valkeea.fishyaddons.vconfig.binding.ConfigBinding.IntBinding;
import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.model.Bounds;
import me.valkeea.fishyaddons.vconfig.ui.model.ClickContext;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import me.valkeea.fishyaddons.vconfig.ui.render.VCRenderContext;

public class ColorControl extends AbstractUIControl {
    
    private final IntBinding colorBinding;
    private final Consumer<ColorControl> colorWheelOpener;
    private int cachedColor;
    
    /**
     * Create a color picker.
     * 
     * @param colorBinding Binding for the color value (ARGB integer)
     * @param colorWheelOpener Open {@link me.valkeea.fishyaddons.vconfig.ui.screen.ColorWheel}
     */
    public ColorControl(
        IntBinding colorBinding,
        Consumer<ColorControl> colorWheelOpener,
        String[] tooltip
    ) {
        this.colorBinding = colorBinding;
        this.colorWheelOpener = colorWheelOpener;
        this.tooltip = tooltip;
    }
    
    @Override
    public void render(VCRenderContext ctx, int x, int y) {

        int size = Dimensions.BUTTON_H; // Square button based on standard height
        
        if (cachedValueDirty) {
            cachedColor = colorBinding.get();
            markCacheFresh();
        }
        
        ctx.context.fill(x, y, x + size, y + size, cachedColor);
        RenderUtils.border(ctx.context, x, y, size, size, 0x80FFFFFF);

        this.lastBounds = new Bounds(x, y, size, size);
    }
    
    @Override
    public boolean handleClick(ClickContext ctx, int x, int y) {
        if (lastBounds == null || !ctx.isLeftClick()) return false;
        if (!lastBounds.contains(ctx.mouseX, ctx.mouseY)) {
            return false;
        }
        
        if (colorWheelOpener != null) {
            colorWheelOpener.accept(this);
        }
        
        return true;
    }
    
    @Override
    public int getPreferredWidth() {
        return Dimensions.BUTTON_H;
    }
    
    public IntBinding getColorBinding() {
        return colorBinding;
    }
}
