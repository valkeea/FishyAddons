package me.valkeea.fishyaddons.vconfig.ui.control;

import me.valkeea.fishyaddons.vconfig.binding.ConfigBinding.StringBinding;
import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.model.Bounds;
import me.valkeea.fishyaddons.vconfig.ui.model.ClickContext;
import me.valkeea.fishyaddons.vconfig.ui.render.VCRenderContext;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCKeybind;
import net.minecraft.client.input.KeyInput;

public class KeybindControl extends AbstractUIControl {
    private final StringBinding keyBinding;
    private VCKeybind widget = null;
    
    public KeybindControl(StringBinding keyBinding, String[] tooltip) {
        this.keyBinding = keyBinding;
        this.tooltip = tooltip;
    }
    
    @Override
    public void render(VCRenderContext ctx, int x, int y) {

        int h = getPreferredHeight();

        if (widget == null) {
            widget = new VCKeybind(keyBinding.get(), x, y, getPreferredWidth(), h);
        } else {
            widget.setHovered(hovered);
            widget.setPosition(x, y);
        }        

        if (cachedValueDirty) {
            var newKey = widget.getKey();            
            keyBinding.set(newKey);            
            markCacheFresh();
        }

        widget.renderWidget(ctx.context, ctx.textRenderer, Dimensions.SCALE);

        int w = widget.getWidth();
        lastBounds = new Bounds(x, y, w, h);
    }
    
    @Override
    public boolean handleClick(ClickContext ctx, int x, int y) {
        if (widget == null || lastBounds == null || !lastBounds.contains(ctx.mouseX, ctx.mouseY)) {   
            return false;
        }
        boolean handled = widget.mouseClicked(ctx.click, ctx.doubled);
        if (handled && !widget.isListening()) {
            cachedValueDirty = true;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean handleKeyPress(KeyInput input) {
        if (widget == null) return false;
        if (widget.keyPressed(input)) {
            cachedValueDirty = true;
            return true;
        }
        return false;
    }
}
