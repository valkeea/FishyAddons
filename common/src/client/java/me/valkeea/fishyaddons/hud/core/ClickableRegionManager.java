package me.valkeea.fishyaddons.hud.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import me.valkeea.fishyaddons.feature.qol.CopyChat;
import me.valkeea.fishyaddons.tool.FishyMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Manages clickable regions for HUD elements (e.g., clickable lines, menu items).
 */
public class ClickableRegionManager {
    private final List<ClickableRegion<?>> regions = new ArrayList<>();
    
    /** Register a clickable region */
    public <T> ClickableRegion<T> addRegion(int x, int y, int width, int height, T data, Consumer<T> onClick) {
        ClickableRegion<T> region = new ClickableRegion<>(x, y, width, height, data, onClick);
        regions.add(region);
        return region;
    }
    
    /** Register a line as clickable */
    public <T> ClickableRegion<T> addLine(int x, int y, int width, int lineHeight, T data, Consumer<T> onClick) {
        return addRegion(x, y, width, lineHeight, data, onClick);
    }
    
    /** Clear all regions */
    public void clear() {
        regions.clear();
    }
    
    /**
     * Handle click on any region, returns true if handled.
     * 
     * All regions add the line to clipboard on right-click if
     * data is Text & button 1 has not been set to primary for that region.
     */
    public boolean handleClick(double mouseX, double mouseY, int button) {
        for (ClickableRegion<?> region : regions) {
            if (region.button == button && region.isHovered(mouseX, mouseY)) {
                region.invokeClick();
                return true;
            }
            if (button == 1 && region.isHovered(mouseX, mouseY) && region.getData() instanceof Text text) {
                CopyChat.toClipboard(text.getString());
                return true;
            }
        }
        return false;
    }
    
    /** Check if mouse is hovering over any region */
    public boolean isHovering(double mouseX, double mouseY) {
        return regions.stream().anyMatch(r -> r.isHovered(mouseX, mouseY));
    }
    
    /** Get region at position */
    @SuppressWarnings("squid:S1452")
    public ClickableRegion<?> getRegionAt(double mouseX, double mouseY) {
        return regions.stream()
            .filter(r -> r.isHovered(mouseX, mouseY))
            .findFirst()
            .orElse(null);
    }
    
    /** Render tooltips for hovered regions */
    public void renderTooltips(DrawContext context, HudDrawer drawer, double mouseX, double mouseY) {
        for (ClickableRegion<?> region : regions) {
            if (region.isHovered(mouseX, mouseY) && region.hasTooltip()) {
                region.renderTooltip(context, drawer, (int)mouseX, (int)mouseY);
                break;
            }
        }
    }
    
    /**
     * Individual clickable region
     */
    public static class ClickableRegion<T> {
        private int x;
        private int y;
        private int width;
        private int height;
        private int button;
        private T data;
        private Consumer<T> onClick;
        private List<Text> tooltip;
        
        public ClickableRegion(int x, int y, int width, int height, T data, Consumer<T> onClick) {
            this(x, y, width, height, 0, data, onClick);
        }
        
        public ClickableRegion(int x, int y, int width, int height, int button, T data, Consumer<T> onClick) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.button = button;
            this.data = data;
            this.onClick = onClick;
        }
        
        public boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width 
                && mouseY >= y && mouseY <= y + height;
        }

        public void invokeClick() {
            if (onClick != null) {
                onClick.accept(data);
            }
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public T getData() { return data; }
        
        /** Set which mouse button this region responds to */
        public ClickableRegion<T> withButton(int button) {
            this.button = button;
            return this;
        }
        
        /** Set tooltip for this region */
        public ClickableRegion<T> withTooltip(List<Text> tooltip) {
            this.tooltip = tooltip;
            return this;
        }
        
        /** Check if this region has a tooltip */
        public boolean hasTooltip() {
            return tooltip != null && !tooltip.isEmpty();
        }
        
        /** Render tooltip at mouse position */
        public void renderTooltip(DrawContext context, HudDrawer drawer, int mouseX, int mouseY) {
            if (hasTooltip()) {
                drawer.drawTooltip(context, tooltip, mouseX, mouseY, FishyMode.getThemeColor());
            }
        }
    }
}
