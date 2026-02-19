package me.valkeea.fishyaddons.ui;

/**
 * Scaled constants for UI dimensions and scaling.
 */
public class VCScaling {
    
    private final float uiScale;
    
    // Scaling thresholds and caps
    private static final float VERTICAL_CAP_THRESHOLD = 0.8f;
    private static final float HORIZONTAL_CAP_THRESHOLD = 0.8f;
    
    public VCScaling(float uiScale) {
        this.uiScale = uiScale;
    }
    
    /**
     * Apply standard scaling for most UI elements
     */
    public int scale(int baseValue) {
        return (int)(baseValue * uiScale);
    }
    
    /**
     * Apply capped vertical scaling to prevent excessive spacing at large scales
     */
    public int scaleVertical(int baseValue) {
        if (uiScale <= VERTICAL_CAP_THRESHOLD) {
            return (int)(baseValue * uiScale);
        } else {
            return (int)(baseValue * VERTICAL_CAP_THRESHOLD + baseValue * 0.2f * (uiScale - VERTICAL_CAP_THRESHOLD));
        }
    }
    
    /**
     * Apply capped horizontal scaling for control areas to save space at large scales
     */
    public int scaleHorizontal(int baseValue) {
        if (uiScale <= HORIZONTAL_CAP_THRESHOLD) {
            return (int)(baseValue * uiScale);
        } else {
            return (int)(baseValue * HORIZONTAL_CAP_THRESHOLD + baseValue * 0.2f * (uiScale - HORIZONTAL_CAP_THRESHOLD));
        }
    }
    
    /**
     * Calculate control area width with horizontal scaling cap
     */
    public int getControlAreaWidth() {
        int baseControlAreaWidth = 140;
        
        if (uiScale < 0.5f) {
            return Math.max(60, (int)(baseControlAreaWidth * uiScale * 0.5f));
        } else if (uiScale < 0.7f) {
            return Math.max(70, (int)(baseControlAreaWidth * uiScale * 0.6f));
        } else {
            return scaleHorizontal(baseControlAreaWidth);
        }
    }
    
    /**
     * Calculate tab area width with scaling constraints
     */
    public int getTabWidth() {
        if (uiScale < 0.6f) {
            return Math.min(50, scale(100));
        } else if (uiScale <= 1.0f) {
            return scale(110);
        } else {
            return Math.max(95, scale(85)); //
        }
    }

    /**
     * Calculate textfield width
     */
    public int getFieldWidth() {
        if (uiScale < 0.5f) {
            return Math.max(40, (int)(VCConstants.BASE_FIELD_WIDTH * uiScale * 0.5f));
        } else if (uiScale < 0.7f) {
            return Math.max(50, (int)(VCConstants.BASE_FIELD_WIDTH * uiScale * 0.6f));
        } else {
            return scale(VCConstants.BASE_FIELD_WIDTH);
        }
    }
    
    /**
     * Calculate standard tab height
     */
    public int getTabHeight() {
        return Math.clamp(scale(25), 8, VCConstants.getSearchHeight(uiScale));
    }

    // === Entry Layout Calculations ===
    
    /**
     * Calculate content X position with indent offset
     */
    public int getContentX(int baseX, boolean isSubEntry) {
        int contentX = baseX + scale(15);
        if (isSubEntry) {
            contentX += scaleVertical(30);
        }
        return contentX;
    }
    
    /**
     * Calculate content Y position
     */
    public int getContentY(int baseY) {
        return baseY + scaleVertical(12);
    }
    
    /**
     * Calculate control Y position with capped scaling
     */
    public int getControlY(int baseY) {
        return baseY + scaleVertical(14);
    }

    /**
     * Calculate list startY
     */
    public int getListStartY() {
        return VCConstants.SEARCH_Y + VCConstants.getSearchHeight(uiScale) + 10;
    }

    /**
     * Calculate scrollbar X position
     */
    public int getScrollbarX(int centerX, int entryWidth) {
        return centerX + entryWidth / 2 + 20;
    }

    /**
     * Calculate background bounds for sub-entries
     */
    public Bounds getSubEntryBgBounds(int x, int y, int entryWidth, int entryHeight) {
        int bgX = x + scale(5);
        int subBgWidth = entryWidth - scale(10);
        return new Bounds(bgX, y, subBgWidth, entryHeight);
    }

    /**
     * Represents a rectangular area with consistent scaling
     */
    public static class Bounds {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public Bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width &&
                   mouseY >= y && mouseY <= y + height;
        }
    }
}