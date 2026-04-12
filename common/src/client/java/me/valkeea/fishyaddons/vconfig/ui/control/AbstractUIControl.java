package me.valkeea.fishyaddons.vconfig.ui.control;

import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.model.Bounds;

public abstract class AbstractUIControl implements UIControl {
    protected boolean hovered = false;
    protected Bounds lastBounds = null;
    protected String[] tooltip = new String[0];
    protected boolean cachedValueDirty = true;
    
    public void updateHover(int mouseX, int mouseY, int x, int y) {
        if (lastBounds != null) {
            hovered = lastBounds.contains(mouseX, mouseY);
        } else {
            hovered = false;
        }
    }

    public Bounds getBounds() {
        return lastBounds;
    }
    
    public boolean isHovered(double mouseX, double mouseY) {
        return hovered;
    }
    
    /**
     * Get tooltip content for this control.
     * 
     * @return Tooltip lines, or empty array if no tooltip
     */
    public String[] getTooltipContent() {
        return tooltip;
    }

    public int getPreferredWidth() {
        return Dimensions.BUTTON_W;
    }
    
    public int getPreferredHeight() {
        return Dimensions.BUTTON_H;
    }    

    protected Bounds calculateBounds(int x, int y) {
        return new Bounds(
            x,
            y,
            getPreferredWidth(),
            getPreferredHeight()
        );
    }

    protected void setHovered(boolean hovered) {
        this.hovered = hovered;
    }
    
    /**
     * Mark the cached value as dirty, forcing a refresh on next render.
     * Called when the screen is initialized or refreshed.
     */
    public void invalidateCachedValue() {
        cachedValueDirty = true;
    }
    
    /**
     * Mark value as fresh after reading from binding.
     */
    protected void markCacheFresh() {
        cachedValueDirty = false;
    }
}
