package me.valkeea.fishyaddons.vconfig.ui.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.layout.Dimensions;
import me.valkeea.fishyaddons.vconfig.ui.model.BaseContext;
import me.valkeea.fishyaddons.vconfig.ui.model.Tab;
import me.valkeea.fishyaddons.vconfig.ui.model.Tab.TabItem;
import me.valkeea.fishyaddons.vconfig.ui.model.VCEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

public class TabManager {
    private final List<Tab> tabs;
    private int activeTabIndex;
    private int tabAreaBottomY = 0;
    
    private final Screen screen;
    private final RenderManager renderManager;
    private final ThemeProvider themeProvider;
    private final NavigationCallback navigationCallback;
    
    @FunctionalInterface
    public interface ThemeProvider {
        Integer getThemeColor();
    }
    
    public interface NavigationCallback {

        /** Navigate to a specific entry */
        void navigateToEntry(VCEntry entry);
        
        /** Check if an entry is a sub-entry */
        boolean isSubEntry(VCEntry entry);
        
        /** Clear search and restore full entry list */
        void clearSearch();
        
        /**
         * Filter entries by category
         * @param category The category to filter by, or null for all
         */
        void filterByCategory(UICategory category);

        /**
         * Find entries by feature name/key
         * @param featureName The feature name to search for
         * @return The list of entries or empty list if not found
         */
        List<VCEntry> findEntriesBy(String featureName);        
        
        /** Navigate to and show only specific entries */
        void showOnlyEntries(List<VCEntry> entries);
    }
    
    public TabManager(Screen screen, RenderManager renderManager, ThemeProvider themeProvider, 
                     NavigationCallback navigationCallback, List<VCEntry> allEntries) {
        this.screen = screen;
        this.renderManager = renderManager;
        this.themeProvider = themeProvider;
        this.navigationCallback = navigationCallback;
        this.tabs = generateTabs(allEntries);
        this.activeTabIndex = 0;
    }
    
    // --- State Management ---
    
    public int getActiveTabIndex() {
        return activeTabIndex;
    }
    
    public Tab getActiveTab() {
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            return tabs.get(activeTabIndex);
        }
        return null;
    }
    
    public List<Tab> getTabs() {
        return new ArrayList<>(tabs);
    }
    
    public boolean isEmpty() {
        return tabs.isEmpty();
    }

    public boolean anyActive() {  
        return activeTabIndex >= 0 && activeTabIndex < tabs.size() && tabs.get(activeTabIndex).isDropdownVisible();
    }

    /** Clear active tab and hide all dropdowns */
    public void clearActive() {
        activeTabIndex = -1;
        hideAllDropdowns();
    }
    
    // --- Coordinate Management ---
    
    /**
     * Update tab coordinates based on layout parameters.
     */
    public void updateTabCoordinates(int startX, int startY, int tabWidth, int tabHeight, int screenHeight) {
        int currentY = startY;
        
        for (int i = 0; i < tabs.size(); i++) {

            Tab tab = tabs.get(i);
            tab.setTabBounds(startX, currentY, tabWidth, tabHeight);
            currentY += tabHeight + 2;
            
            if (i == activeTabIndex && tab.isDropdownVisible()) {
                List<TabItem> items = tab.getDropdownItems();
                int itemHeight = Dimensions.TAB_ITEM_H;
                int padding = Dimensions.TAB_BORDER_OFFSET;
                int maxDropdownHeight = screenHeight - currentY - Dimensions.END_Y_OFFSET;
                int actualDropdownHeight = Math.min(maxDropdownHeight, (items.size() * itemHeight) + padding * 2);
                
                tab.setDropdownBounds(startX, currentY, tabWidth, actualDropdownHeight);
                currentY += actualDropdownHeight + 4;
            }
        }
        
        // Bottom Y stored for entry info
        tabAreaBottomY = currentY;
    }
    
    // --- Rendering ---
    
    /** Render all tabs and their dropdowns */
    public void render(DrawContext context, int mouseX, int mouseY, int startX, 
                      int tabAreaWidth) {
        if (tabs.isEmpty()) return;
        
        BaseContext renderCtx = new BaseContext(context, mouseX, mouseY, startX, tabAreaWidth);
        renderManager.renderTabsWithCoordinates(renderCtx, screen, tabs, activeTabIndex, 
                                          themeProvider.getThemeColor());
    }
    
    // --- Interaction ---
    
    public boolean handleClick(double mouseX, double mouseY) {
        if (tabs.isEmpty()) return false;
        
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            
            if (tab.isDropdownVisible() && tab.isPointInDropdown(mouseX, mouseY)) {
                handleDropdownClick(tab, mouseY);
                return true;
            }
            
            if (tab.isPointInTab(mouseX, mouseY)) {
                return handleTabClick(i, tab);
            }
        }
        
        return false;
    }
    
    private void handleDropdownClick(Tab tab, double mouseY) {
        TabItem item = tab.getClickedDropdownItem(mouseY);
        if (item == null) return;
        List<VCEntry> entries = navigationCallback.findEntriesBy(item.navigationKey());
        if (entries == null || entries.isEmpty()) return;
        navigationCallback.showOnlyEntries(entries);
    }
    
    private boolean handleTabClick(int index, Tab tab) {

        if (activeTabIndex == index) {
            if (tab.isDropdownVisible()) {
                tab.hideDropdown();
                navigationCallback.filterByCategory(null);
                activeTabIndex = -1;
                return true;
            } else {
                tab.showDropdown();
            }
        } else {
            navigateToTab(index);
        }
        
        return false;
    }
    
    // --- Navigation ---
    
    /**
     * Navigate to a specific tab by index.
     * Filters entries to show only the selected category.
     * 
     * @param tabIndex The index of the tab to navigate to
     */
    public void navigateToTab(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= tabs.size()) return;
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            tabs.get(activeTabIndex).hideDropdown();
        }
        activeTabIndex = tabIndex;
        Tab tab = tabs.get(tabIndex);

        navigationCallback.filterByCategory(tab.category);
        navigationCallback.clearSearch();
        tab.showDropdown();
    }
    
    /**
     * Navigate to the next tab
     */
    public void nextTab() {
        int nextIndex = (activeTabIndex + 1) % tabs.size();
        navigateToTab(nextIndex);
    }
    
    /**
     * Navigate to the previous tab
     */
    public void previousTab() {
        int prevIndex = activeTabIndex - 1;
        if (prevIndex < 0) {
            prevIndex = tabs.size() - 1;
        }
        navigateToTab(prevIndex);
    }
    
    /**
     * Hide all dropdowns
     */
    public void hideAllDropdowns() {
        for (Tab tab : tabs) {
            tab.hideDropdown();
        }
    }
    
    /**
     * Get the Y position of the bottom of the tab area (including active dropdowns).
     * 
     * @return The Y coordinate of the bottom of the tab area
     */
    public int getTabAreaBottomY() {
        return tabAreaBottomY;
    }

    /**
     * Dynamically build tabs based on category headers in the entry list.
     * Entries with names containing *text* will be grouped under "text" dropdown items.
     * 
     * @param allEntries The complete list of config entries
     * @return List of tabs with automatic dropdown items
     */
    public List<Tab> generateTabs(List<VCEntry> allEntries) {

        List<Tab> generated = new ArrayList<>();
        UICategory currentCategory = null;
        Tab currentTab = null;
        Map<String, List<VCEntry>> groupedEntries = new java.util.LinkedHashMap<>();
        
        for (VCEntry e : allEntries) {
            // Check if this is a category header
            if (e.isHeader() &&  e.category != null) {
                // Save previous tab if exists
                if (currentTab != null && !groupedEntries.isEmpty()) {
                    List<TabItem> items = buildDropdownItems(groupedEntries);
                    currentTab.setDropdownItems(items);
                    generated.add(currentTab);
                }
                
                // Start new tab
                currentCategory = e.category;
                currentTab = new Tab(e.name, currentCategory);
                groupedEntries = new java.util.LinkedHashMap<>();
                
            } else if (validForExistingCategory(currentTab, currentCategory, e)) {
                String tabItemName = extractTabItemName(e.name);
                
                if (tabItemName != null) {
                    groupedEntries.computeIfAbsent(tabItemName, k -> new ArrayList<>()).add(e);
                }
            }
        }
        
        // Add final tab
        if (currentTab != null && !groupedEntries.isEmpty()) {
            List<TabItem> items = buildDropdownItems(groupedEntries);
            currentTab.setDropdownItems(items);
            generated.add(currentTab);
        }
        
        return generated;
    }

    private static boolean validForExistingCategory(Tab current, UICategory category, VCEntry e) {
        return (current != null && e.category != null && e.category.equals(category) &&
        !e.isHeader() && (e.hasControls() || e.hasSubEntries()));
    }    
    
    /**
     * Returns text between first pair of asterisks.
     * 
     * @param entryName The entry name to parse
     * @return The extracted tab item name, or null if no markers found
     */
    private String extractTabItemName(String entryName) {
        if (entryName == null) return null;
        int first = entryName.indexOf('*');
        if (first == -1) return null;
        int closing = entryName.indexOf('*', first + 1);
        if (closing == -1) return null;
        return entryName.substring(first + 1, closing);
    }
    
    /**
     * Build dropdown items from grouped entries.
     * Each group (by tab item name) becomes a single dropdown item that can navigate to any of its entries.
     * For containers, include navigation to child entries.
     * 
     * @param groupedEntries Map of tab item name -> list of entries with that tab item
     * @return List of tab items
     */
    private List<TabItem> buildDropdownItems(
            Map<String, List<VCEntry>> groupedEntries) {
        
        List<TabItem> items = new ArrayList<>();
        
        for (var group : groupedEntries.entrySet()) {
            String tabItemName = group.getKey();
            List<VCEntry> entries = group.getValue();
            
            if (entries.isEmpty()) continue;
            
            // Use the first entry as the primary navigation target
            VCEntry primaryEntry = entries.get(0);
            // Actual entry name for navigation (not transformed)
            String navigationKey = primaryEntry.name;
            
            items.add(new TabItem(tabItemName, navigationKey));
        }
        
        return items;
    }
}
