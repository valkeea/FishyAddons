package me.valkeea.fishyaddons.vconfig.ui.layout;

import me.valkeea.fishyaddons.vconfig.ui.model.Bounds;

public final class Dimensions {
    // Known UI Position Constants
    public static final short SEARCH_Y = 40;
    public static final short HEADER_Y = 20;

    // Base entry dimensions
    public static final int ENTRY_W = 650;    
    public static final int ENTRY_H = 65;
    public static final int SUB_ENTRY_H = 35;
    public static final int HEADER_H = 40;

    // Control dimensions
    public static final int BUTTON_W = 24;    
    public static final int BUTTON_H = 16;
    public static final int CONTROL_W = 80;    
    public static final int CONTROL_H = BUTTON_H;      
    public static final int FIELD_W = CONTROL_W;
    public static final int SEARCH_H = 20;   
    public static final int SLIDER_W = 70;
    public static final int SLIDER_H = 12;    

    // Tabs
    public static final int TAB_W = 100;
    public static final int TAB_H = SEARCH_H;
    public static final int TAB_ITEM_H = 14;
    public static final int TAB_ITEM_PADDING = 4;
    public static final int TAB_TEXT_OFFSET_X = 6;
    public static final int TAB_INDICATOR_OFFSET_X = 12; 
    public static final int TAB_BORDER_OFFSET = 2;
    public static final int TAB_ENTRY_GAP = 10;

    // Scaling factors
    public static final float REDUCTION_MUL = 0.8f;
    public static final float EXREME_REDUCTION_MUL = 0.02f;
    public static final float SCALE = 1.0f;
    
    // Controls
    public static final int CONTROL_GAP = 5;
    public static final int CONTROL_OUTDENT = 30;
    public static final int SUB_CONTROL_OUTDENT = 15;
    
    // Text layout and positioning
    public static final int DESC_OFFSET = 20;
    public static final int DESC_LINE_SPACING = 12;
    
    // Sub-entry specific
    public static final int SUB_HEADER_HORIZONTAL_OFFSET = 15;
    public static final int SUB_ENTRY_VERTICAL_OFFSET = 10;
    
    // Header separator gaps
    public static final int HEADER_GAP_PADDING = 12;
    
    // --- Layout Constants ---
    
    // Button dimensions
    public static final int END_Y_OFFSET = 5;
    
    // Scrollbar dimensions
    public static final int SCROLLBAR_W = 4;
    public static final int SCROLLBAR_THUMB_MIN_H = 10;
    public static final int SCROLL_GAP = BUTTON_W * 2;
    
    // Icon dimensions
    public static final int MIN_ICON_SIZE = 8;
    public static final int MAX_ICON_SIZE = 32;
    
    // Sub-entry separator
    public static final int SEPARATOR_H = 2;
    public static final int SEPARATOR_LINE_H = 1;
    public static final int SUB_SEPARATOR_X_OFFSET = 5;

    public static final int TOTAL_LIST_WIDTH = ENTRY_W + TAB_W + TAB_ENTRY_GAP + SCROLL_GAP + SCROLLBAR_W;

    /**
     * Apply capped vertical scaling to prevent excessive spacing at large scales
     */
    public static int reduce(int baseValue) {
        return (int)(baseValue * REDUCTION_MUL + baseValue * EXREME_REDUCTION_MUL);
    }
    
    public static int getControlY(int baseY, boolean isSubEntry) {
        int controlOffset = BUTTON_H / 2;
        int section = getEntrySectionH(isSubEntry);
        return baseY - controlOffset + section * 2;

    }

    public static int getListStartY() {
        return SEARCH_Y + 10;
    }

    // --- Sub-entry Layout Methods ---

    public static Bounds getSubEntryBgBounds(int x, int y, int entryWidth, int entryHeight) {
        int bgX = x + 5;
        int edge = 10;
        return new Bounds(bgX, y, entryWidth - edge, entryHeight);
    }

    public static int getCustomButtonW(int textWidth) {
        return textWidth + CONTROL_GAP;
    }

    // --- Tab Calculations ---

    public static int getTabTextX(int tabX) {
        return tabX + TAB_TEXT_OFFSET_X;
    }

    /** Get the state indicator X position for a tab */
    public static int getTabIndicatorX(int tabX, int tabWidth) {
        return tabX + tabWidth - TAB_INDICATOR_OFFSET_X;
    }

    // --- Entry Dimensions ---

    public static int getEntryH(boolean isSubEntry) {
        return isSubEntry ? SUB_ENTRY_H : ENTRY_H;
    }

    public static int getEntrySectionH(boolean isSubEntry) {
        return getEntryH(isSubEntry) / 5;
    }

    /** Calculate metadata X position with indent offset */
    public static int getMetadataX(int baseX, boolean isSubEntry) {
        int contentX = baseX + 15;
        if (isSubEntry) {
            contentX += reduce(30);
        }
        return contentX;
    }

    public static int getMetadataY(int baseY, boolean isSubEntry) {
        int section = getEntrySectionH(isSubEntry);
        return baseY + section;
    }        

    public static int getTallSeparatorH() {
        return Math.max(1, SEPARATOR_H);
    }
    
    public static int getSmallSeparatorH() {
        return Math.max(1, SEPARATOR_LINE_H);
    }
    
    // --- Text layout ---
    
    /** Get the Y offset for description text from entry name */
    public static int getDescriptionOffset() {
        return reduce(DESC_OFFSET);
    }

    public static int getIconSize(int baseSize) {
        return Math.clamp(baseSize, MIN_ICON_SIZE, MAX_ICON_SIZE);
    }    

    public static int getHeaderH(boolean isSubEntry) {
        int base = HEADER_H;
        return isSubEntry ? (int)Math.ceil(base / 1.5) : base;
    }
    
    /** Get the Y offset for main header text */
    public static int getHeaderTextY(boolean isSubEntry) {
        int section = getHeaderH(isSubEntry);
        return (int)Math.ceil(section / 1.5);
    }

    public static int getTotalW() {
        return ENTRY_W + SCROLLBAR_W + TAB_W + SCROLL_GAP;
    }    
    
    private Dimensions() {}
}
