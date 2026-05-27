package me.valkeea.fishyaddons.vconfig.ui.manager;

import java.util.List;

import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.model.VCEntry;

public class LayoutManager {
    
    private final int screenWidth;
    private final int screenHeight;
    private final float uiScale;
    
    // Calculated dimensions
    private int entryWidth;
    private int entryHeight;
    private int subEntryHeight;
    private int headerHeight;
    private int tabAreaWidth;
    private int maxVisibleEntries;
    private int endY;
    
    // Layout regions
    private int listStartY;
    private int centerX;
    private int entryCenterX;
    
    public LayoutManager(int screenWidth, int screenHeight, float uiScale) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.uiScale = uiScale;
        
        calcLayout();
    }
    
    /**
     * Calculate all layout dimensions based on screen size and UI scale
     */
    private void calcLayout() {

        entryHeight = Dimensions.getEntryH(false);
        entryWidth = Dimensions.ENTRY_W;
        subEntryHeight = Dimensions.getEntryH(true);
        headerHeight = Dimensions.getHeaderH(true);
        tabAreaWidth = Dimensions.TAB_W;
        
        centerX = screenWidth / 2;
        listStartY = Dimensions.getListStartY();

        entryCenterX = getEntryX() + entryWidth / 2;
        
        int searchY = Dimensions.SEARCH_Y;
        int searchH = Dimensions.SEARCH_H;
        int btnH = Dimensions.BUTTON_H;
        int btnMargin = Dimensions.CONTROL_GAP;
        int availableH = screenHeight - searchY - searchH - btnH - btnMargin;
        
        int btnY = screenHeight - btnH;
        endY = btnY - Dimensions.END_Y_OFFSET;
        
        int avgEntryH = calcAvgEntryHeight();
        maxVisibleEntries = Math.max(1, availableH / avgEntryH);
    }
    
    /**
     * Get the height of a specific entry
     */
    public int getEntryH(boolean isSubEntry) {
        return isSubEntry ? subEntryHeight : entryHeight;
    }
    
    /**
     * Calculate average entry height for layout calculations
     */
    private int calcAvgEntryHeight() {
        return (int)(entryHeight * 0.7f + subEntryHeight * 0.3f);
    }
    
    /**
     * Calculate maximum number of entries that can be displayed
     * Takes into account sub-entry separators and actual entry heights
     */
    @SuppressWarnings("squid:S135")
    public int calcAllowedEntries(List<VCEntry> visibleEntries, EntryTypeChecker checker) {
        int currentY = listStartY;
        int count = 0;
        
        for (int i = 0; i < visibleEntries.size(); i++) {
            VCEntry e = visibleEntries.get(i);
            boolean isSubEntry = checker.isSubEntry(e);
            int currentEntryH = getEntryH(isSubEntry);
            
            if (currentY + currentEntryH > endY) break;
            currentY += currentEntryH;
            count++;
            
            if (shouldDrawSubEnd(visibleEntries, i, checker)) {
                int separatorH = headerHeight;
                if (currentY + separatorH > endY) {
                    break;
                }
                currentY += separatorH;
            }
        }
        
        // Reserve space for 1-2 extra "empty" entries to prevent awkward cutoff
        int reserved = (int)(entryHeight * 1.5f);
        int available = endY - currentY;
        
        if (available > reserved) {
            count -= 1;
        }
        
        return Math.max(1, count);
    }
    
    /**
     * Check if a sub-entry separator should be drawn after the given index
     */
    private boolean shouldDrawSubEnd(List<VCEntry> visible, int currentIdx, EntryTypeChecker checker) {
        if (currentIdx >= visible.size() - 1) {
            return false;
        }
        
        VCEntry current = visible.get(currentIdx);
        VCEntry next = visible.get(currentIdx + 1);
        
        if ((checker.isSubEntry(current) && !checker.isSubEntry(next)) || next == null) {
            return true;
        }
        
        return currentIdx == visible.size() - 1 && checker.isSubEntry(current);
    }
    
    /** Get the X position for the left edge of entries */
    public int getEntryX() {
        return centerX - Dimensions.getTotalW() / 2 + Dimensions.TAB_W + Dimensions.TAB_ENTRY_GAP;
    }

    public int getEntryCenterX() {
        return entryCenterX;
    }
    
    /** Get the X position for the scrollbar */
    public int getScrollbarX() {
        return getEntryX() + entryWidth + Dimensions.SCROLL_GAP;
    }
    
    /** Calculate scrollbar bounds for rendering */
    public KnobBounds getKnobBounds(int actualMaxVisible) {
        int scrollbarX = getScrollbarX();
        int scrollbarWidth = Dimensions.SCROLLBAR_W;
        int indicatorHeight = actualMaxVisible * entryHeight;
        
        return new KnobBounds(
            scrollbarX,
            listStartY,
            scrollbarWidth,
            indicatorHeight
        );
    }

    // Get slightly wider click area for scrollbar to improve usability
    public KnobBounds getStretchedBounds(int actualMaxVisible) {
        KnobBounds bounds = getKnobBounds(actualMaxVisible);
        int extraWidth = Dimensions.SCROLLBAR_W;
        return new KnobBounds(
            bounds.x - extraWidth,
            bounds.y,
            bounds.width + extraWidth,
            bounds.height
        );
    }
    
    /** Calculate scrollbar position and size */
    public ScrollKnob calcScrollKnob(int scrollOffset, int totalEntries, int maxVisible) {
        if (totalEntries <= maxVisible) return null;
        
        var bounds = getKnobBounds(maxVisible);
        int thumbHeight = Math.max(
            Dimensions.SCROLLBAR_THUMB_MIN_H,
            (maxVisible * bounds.height) / totalEntries
        );
        
        int thumbY = bounds.y + (scrollOffset * (bounds.height - thumbHeight)) / (totalEntries - maxVisible);
        
        return new ScrollKnob(
            bounds.x + 1,
            thumbY,
            bounds.width - 2,
            thumbHeight
        );
    }
    
    /** Clamp scroll offset to valid range */
    public int clampScrollOffset(int scrollOffset, int totalEntries, int maxVisible) {
        int maxScroll = Math.max(0, totalEntries - maxVisible);
        return Math.clamp(scrollOffset, 0, maxScroll);
    }
    
    // --- Getters for layout dimensions and positions ---
    
    public int getEntryW() { return entryWidth; }
    public int getEntryH() { return entryHeight; }
    public int getHeaderH(boolean isSubEntry) { return Dimensions.getHeaderH(isSubEntry); }
    public int getTabAreaW() { return tabAreaWidth; }
    public int getAllowedEntries() { return maxVisibleEntries; }
    public int getEndY() { return endY; }
    public int getListStartY() { return listStartY; }
    public int getCenterX() { return centerX; }
    public float getUIScale() { return uiScale; }
    
    // --- Component dimensions and positions ---

    public static class KnobBounds {
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        
        public KnobBounds(int x, int y, int width, int height) {
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
    
    public static class ScrollKnob {
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        
        public ScrollKnob(int x, int y, int width, int height) {
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
    
    @FunctionalInterface
    public interface EntryTypeChecker {
        boolean isSubEntry(VCEntry entry);
    }
}
