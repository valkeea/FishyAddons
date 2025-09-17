package me.valkeea.fishyaddons.ui;

/**
 * Centralized constants for UI dimensions and scaling.
 */
public final class VCConstants {

    // Screen dimensions for scaling calculations based on original design
    public static final int BASE_WIDTH = 1920;
    public static final int BASE_HEIGHT = 1080;
    
    // Base entry dimensions
    public static final int BASE_ENTRY_HEIGHT = 65;
    public static final int BASE_SUB_ENTRY_HEIGHT = 40;
    public static final int BASE_HEADER_HEIGHT = 35;
    public static final int BASE_ENTRY_WIDTH = 800;
    public static final int BASE_SEARCH_HEIGHT = 24;
    
    // Minimum dimensions to ensure usability at very small scales
    public static final int MIN_ENTRY_HEIGHT = 35;
    public static final int MIN_SUB_ENTRY_HEIGHT = 22;
    public static final int MIN_HEADER_HEIGHT = 18;
    public static final int MIN_SEARCH_HEIGHT = 8;
    
    // Dynamic minimum thresholds for very small UI scales
    public static final int TINY_SCALE_MIN_ENTRY = 25;
    public static final int TINY_SCALE_MIN_SUB = 18;
    public static final int TINY_SCALE_MIN_HEADER = 15;
    public static final int TINY_SCALE_MIN_SEARCH = 12;

    // UI scale thresholds for design layout transitions
    public static final float TINY_SCALE_THRESHOLD = 0.4f;
    public static final float SMALL_SCALE_THRESHOLD = 0.5f;
    public static final float COMPACT_SCALE_THRESHOLD = 0.7f;
    
    // Entry width calculations
    public static final int MIN_DYNAMIC_WIDTH_SMALL = 200;
    public static final int MIN_DYNAMIC_WIDTH_NORMAL = 300;
    
    // Utility methods for dimension calculations
    public static int getEntryHeight(float uiScale) {
        float scale = Math.min(uiScale, 1.0f);
        int dynamicMin = uiScale < TINY_SCALE_THRESHOLD ? TINY_SCALE_MIN_ENTRY : MIN_ENTRY_HEIGHT;
        return Math.max(dynamicMin, (int) (BASE_ENTRY_HEIGHT * scale));
    }
    
    public static int getSubEntryHeight(float uiScale) {
        float scale = Math.min(uiScale, 1.0f);
        int dynamicMin = uiScale < TINY_SCALE_THRESHOLD ? TINY_SCALE_MIN_SUB : MIN_SUB_ENTRY_HEIGHT;
        return Math.max(dynamicMin, (int) (BASE_SUB_ENTRY_HEIGHT * scale));
    }
    
    public static int getHeaderHeight(float uiScale) {
        float scale = Math.min(uiScale, 1.0f);
        int dynamicMin = uiScale < TINY_SCALE_THRESHOLD ? TINY_SCALE_MIN_HEADER : MIN_HEADER_HEIGHT;
        return Math.max(dynamicMin, (int) (BASE_HEADER_HEIGHT * scale));
    }
    
    public static int getEntryWidth(float uiScale) {
        float scale = Math.min(uiScale, 1.0f);
        int scaledWidth = (int) (BASE_ENTRY_WIDTH * scale);
        int dynamicMinWidth = uiScale < SMALL_SCALE_THRESHOLD ? MIN_DYNAMIC_WIDTH_SMALL : MIN_DYNAMIC_WIDTH_NORMAL;
        return Math.max(dynamicMinWidth, scaledWidth);
    }
    
    public static int getSearchHeight(float uiScale) {
        float scale = Math.min(uiScale, 1.0f);
        int dynamicMin = uiScale < TINY_SCALE_THRESHOLD ? TINY_SCALE_MIN_SEARCH : MIN_SEARCH_HEIGHT;
        return Math.max(dynamicMin, (int) (BASE_SEARCH_HEIGHT * scale));
    }
    
    private VCConstants() {
        throw new UnsupportedOperationException("UIConstants is a utility class and should not be instantiated");
    }
}
