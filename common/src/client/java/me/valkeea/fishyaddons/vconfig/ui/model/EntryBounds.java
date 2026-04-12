package me.valkeea.fishyaddons.vconfig.ui.model;

public record EntryBounds(
    VCEntry entry,
    int x,
    int y,
    int height,
    int index,
    boolean isSubEntry,
    boolean needsSeparator
) {
    
    /**
     * Check if a Y coordinate is within this entry's bounds.
     */
    public boolean contains(double mouseY) {
        return mouseY >= y && mouseY < y + height;
    }
    
    /**
     * Get the bottom Y coordinate of this entry.
     */
    public int getEndY() {
        return y + height;
    }
}
