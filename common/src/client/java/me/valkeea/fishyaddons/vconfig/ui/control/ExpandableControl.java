package me.valkeea.fishyaddons.vconfig.ui.control;

import me.valkeea.fishyaddons.vconfig.binding.ConfigBinding.DummyBinding;
import me.valkeea.fishyaddons.vconfig.ui.layout.Colors;
import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.model.Bounds;
import me.valkeea.fishyaddons.vconfig.ui.model.ClickContext;
import me.valkeea.fishyaddons.vconfig.ui.render.RenderUtils;
import me.valkeea.fishyaddons.vconfig.ui.render.VCRenderContext;
import me.valkeea.fishyaddons.vconfig.ui.render.VCText;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCButton;

/**
 * A simple entry container with visual feedback for expanded state.
 * 
 * <p>The actual expansion state is tracked by
 * {@link me.valkeea.fishyaddons.vconfig.ui.screen.VCScreen} expandedEntries map.
 * 
 * <p>{@link #expandedBinding} serves as a bridge for screen state management.
 */
public class ExpandableControl extends AbstractUIControl {
    private static final int BASE_HEIGHT = Dimensions.CONTROL_H + 2;
    
    private final DummyBinding state;
    private final boolean secondary;
    
    private int controlWidth = 0;
    private boolean cachedExpanded;

    /**
     * @param expandedBinding Syncs with VCScreen's expansion state
     * @param tooltip The tooltip text to display on hover
     */
    public ExpandableControl(DummyBinding state, String[] tooltip, boolean secondary) {
        this.state = state;
        this.tooltip = tooltip;
        this.secondary = secondary;
    }
    
    @Override
    public void render(VCRenderContext ctx, int x, int y) {
        
        if (cachedValueDirty) {
            cachedExpanded = state.get();
            markCacheFresh();
        }
        
        var text = cachedExpanded ? "Collapse" : "Expand";
        int textWidth = ctx.textRenderer.getWidth(text);

        if (controlWidth == 0) controlWidth = textWidth;
        
        int w = getPreferredWidth();
        int h = getPreferredHeight();

        if (!secondary) x = x + Dimensions.BUTTON_W + Dimensions.CONTROL_GAP;

        boolean btnHovered = VCButton.isHovered(x, y, w, h, ctx.mouseX, ctx.mouseY);
        int triangleColor = btnHovered ? 0xFFA3FFFF : Colors.AQUA;

        RenderUtils.gradientTriangle(
            ctx.context,
            x, y + h / 2,
            w, h / 2,
            triangleColor,
            isExpanded()
        );
        
        int textX = x + (w / 2) - (int)(textWidth * Dimensions.SCALE / 2);

        VCText.flatText(
            ctx.context,
            ctx.textRenderer,
            text, textX, y,
            ctx.themeColor
        );
        
        lastBounds = new Bounds(x, y, w, h);
    } 
    
    @Override
    public boolean handleClick(ClickContext context, int x, int y) {
        if (hovered) {
            cachedExpanded = !cachedExpanded;
            state.set(cachedExpanded);
            markCacheFresh();
            return true;
        }
        return false;
    }
    
    @Override
    public int getPreferredWidth() {
        return Dimensions.BUTTON_W + Dimensions.SUB_CONTROL_OUTDENT;
    }
    
    @Override
    public int getPreferredHeight() {
        return BASE_HEIGHT;
    }
    
    public boolean isExpanded() {
        return cachedExpanded;
    }
}
