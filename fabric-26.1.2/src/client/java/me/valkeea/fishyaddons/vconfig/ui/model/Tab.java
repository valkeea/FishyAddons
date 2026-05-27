package me.valkeea.fishyaddons.vconfig.ui.model;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;

public class Tab {
    public final String headerText;
    public final UICategory category;
    public final String displayName;
    
    // Layout
    private int x;
    private int y;
    private int width;
    private int height;
    private int dropdownX;
    private int dropdownY;
    private int dropdownWidth;
    private int dropdownHeight;
    
    private boolean dropdownVisible;

    public record TabItem(String displayName, String navigationKey) {}
    private List<TabItem> dropdownItems;    
    
    public Tab(String headerText, UICategory category) {
        this.headerText = headerText;
        this.category = category != null ? category : UICategory.NONE;
        this.displayName = this.category.shortName();
        this.dropdownVisible = false;
        this.dropdownItems = new ArrayList<>();
    }
    
    public void setTabBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    public void setDropdownBounds(int x, int y, int width, int height) {
        this.dropdownX = x;
        this.dropdownY = y;
        this.dropdownWidth = width;
        this.dropdownHeight = height;
    }
    
    public boolean isPointInTab(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;
    }
    
    public boolean isPointInDropdown(double mouseX, double mouseY) {
        return dropdownVisible &&
                mouseX >= dropdownX && mouseX <= dropdownX + dropdownWidth &&
                mouseY >= dropdownY && mouseY <= dropdownY + dropdownHeight;
    }
    
    // --- Dropdown management ---
    
    public void setDropdownItems(List<TabItem> items) {
        this.dropdownItems = new ArrayList<>(items);
    }
    
    public List<TabItem> getDropdownItems() {
        return new ArrayList<>(dropdownItems);
    }
    
    public void showDropdown() {
        this.dropdownVisible = true;
    }
    
    public void hideDropdown() {
        this.dropdownVisible = false;
    }
    
    public boolean isDropdownVisible() {
        return dropdownVisible;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getDropdownX() { return dropdownX; }
    public int getDropdownY() { return dropdownY; }
    public int getDropdownWidth() { return dropdownWidth; }
    public int getDropdownHeight() { return dropdownHeight; }
    
    /**
     * Find a clicked item based on Y coordinate
     */
    public TabItem getClickedDropdownItem(double mouseY) {
        if (!dropdownVisible || dropdownItems.isEmpty()) return null;
        
        int itemHeight = Dimensions.TAB_ITEM_H;
        int padding = Dimensions.TAB_ITEM_PADDING;
        int listStartY = dropdownY + padding;
        
        for (int i = 0; i < dropdownItems.size(); i++) {
            int itemY = listStartY + (i * itemHeight);
            if (mouseY >= itemY && mouseY <= itemY + itemHeight) {
                return dropdownItems.get(i);
            }
        }
        return null;
    }
}
