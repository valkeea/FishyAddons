package me.valkeea.fishyaddons.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.valkeea.fishyaddons.util.text.Color;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

public class VCCategory {
    /**
     * Represents a dropdown item with separate display and feature names
     */
    public static class DropdownItem {
        public final String displayName;
        public final String navigationKey;

        public DropdownItem(String displayName, String navigationKey) {
            this.displayName = displayName;
            this.navigationKey = navigationKey;
        }

        // Used when display and feature names are the same
        public DropdownItem(String name) {
            this.displayName = name;
            this.navigationKey = name.toLowerCase();
        }
    }

    /**
     * Represents a navigation tab for VCScreen
     */
    public static class Tab {
        public final String name;
        public final String shortName;
        public final String headerText;
        
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
        private List<DropdownItem> dropdownItems;

        public Tab(String name, String shortName, String headerText) {
            this.name = name;
            this.shortName = shortName;
            this.headerText = headerText;
            this.dropdownVisible = false;
            this.dropdownItems = new ArrayList<>();
        }
        
        // === Coordinate Management ===
        
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
        
        // === Dropdown Management ===
        
        public void setDropdownItems(List<DropdownItem> items) {
            this.dropdownItems = new ArrayList<>(items);
        }
        
        public List<DropdownItem> getDropdownItems() {
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
        
        // === Getters ===
        
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getDropdownX() { return dropdownX; }
        public int getDropdownY() { return dropdownY; }
        public int getDropdownWidth() { return dropdownWidth; }
        public int getDropdownHeight() { return dropdownHeight; }
        
        /**
         * Find dropdown item that was clicked based on Y coordinate
         */
        public DropdownItem getClickedDropdownItem(double mouseY, VCScaling scaling) {
            if (!dropdownVisible || dropdownItems.isEmpty()) return null;
            
            int itemHeight = scaling.scale(14);
            int padding = scaling.scale(8);
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
    
    /**
     * Render vertical tabs
     */
    protected static void renderTabsWithCoordinates(VCContext renderCtx, Screen screen, List<VCCategory.Tab> tabs, int activeTabIndex, Integer theme, float uiScale) {
        for (int i = 0; i < tabs.size(); i++) {
            VCCategory.Tab tab = tabs.get(i);
            boolean isActive = i == activeTabIndex;
            boolean isHovered = tab.isPointInTab(renderCtx.mouseX, renderCtx.mouseY);

            renderTabWithCoordinates(renderCtx, screen, tab, isActive, isHovered, theme, uiScale);

            if (tab.isDropdownVisible()) {
                renderTabDropdownWithCoordinates(renderCtx, screen, tab, theme, uiScale);
            }
        }
    }

    private static void renderTabWithCoordinates(VCContext renderCtx, Screen screen, VCCategory.Tab tab, boolean isActive,
                                            boolean isHovered, Integer theme,  float uiScale) {
        int x = tab.getX();
        int y = tab.getY();
        int width = tab.getWidth();
        int height = tab.getHeight();
        
        int bgColor;
        if (isActive) {
            bgColor = 0x60555555;
        } else if (isHovered) {
            bgColor = 0x40333333;
        } else {
            bgColor = 0x30222222;
        }

        renderCtx.context.fill(x, y, x + width, y + height, bgColor);
        
        if (isActive) {
            renderCtx.context.fill(x, y, x + 2, y + height, theme);
        }
        
        String displayText = uiScale < 0.7f ? tab.shortName : tab.name;
        int textColor;

        if (isActive) {
            textColor = theme;
        } else if (isHovered) {
            textColor = 0xFFBBBBBB;
        } else {
            textColor = 0xFF888888;
        }

        int textX = x + (int)(6 * uiScale);
        int textY = y + (height - (int)(screen.getTextRenderer().fontHeight * uiScale)) / 2;
        
        VCText.drawScaledText(renderCtx.context, screen.getTextRenderer(), displayText, textX, textY, textColor, uiScale);
        
        int arrowX = x + width - (int)(12 * uiScale);
        int arrowY = y + height / 2;
        int arrowSize = Math.max(2, (int)(3 * uiScale));
        renderDropdownArrow(renderCtx.context, arrowX, arrowY, arrowSize, textColor, tab.isDropdownVisible());
    }

    private static void renderTabDropdownWithCoordinates(VCContext renderCtx, Screen screen, VCCategory.Tab tab, Integer theme, float uiScale) {
        int x = tab.getDropdownX();
        int y = tab.getDropdownY();
        int width = tab.getDropdownWidth();
        int height = tab.getDropdownHeight();
        
        renderCtx.context.fill(x, y, x + width, y + height, 0x90000000);
        VCRenderUtils.border(renderCtx.context, x, y, width, height, Color.mulRGB(theme, 0.6f));
        
        List<VCCategory.DropdownItem> items = tab.getDropdownItems();
        int itemHeight = (int)(14 * uiScale);
        int padding = (int)(8 * uiScale);
        int listY = y + padding;
        
        for (int i = 0; i < items.size() && listY < y + height - padding; i++) {
            VCCategory.DropdownItem item = items.get(i);
            boolean isHovered = renderCtx.mouseX >= x + padding && 
                               renderCtx.mouseX <= x + width - padding &&
                               renderCtx.mouseY >= listY && renderCtx.mouseY <= listY + itemHeight;
            
            int textColor = isHovered ? theme : 0xFFCCCCCC;
            if (isHovered) {
                renderCtx.context.fill(x + padding, listY, x + width - padding, 
                                     listY + itemHeight, 0x30FFFFFF);
            }
            
            VCText.drawScaledText(renderCtx.context, screen.getTextRenderer(), "• " + item.displayName, 
                                 x + (int)(6 * uiScale), listY + (int)(2 * uiScale), textColor, uiScale);
            listY += itemHeight;
        }
    }
    
    private static void renderDropdownArrow(DrawContext context, int x, int y, int size, int color, boolean up) {
        if (up) {
            for (int i = 0; i < size; i++) {
                context.fill(x - i, y - i, x + i + 1, y - i + 1, color);
            }
        } else {
            for (int i = 0; i < size; i++) {
                context.fill(x - i, y + i, x + i + 1, y + i + 1, color);
            }
        }
    }

    /**
     * Creates and returns the list of navigation tabs based on main sections.
     */
    protected static List<Tab> createTabs() {
        List<Tab> tabs = new ArrayList<>();
        
        var interfaceTab = new Tab("Interface", "UI", "── Interface ──");
        interfaceTab.setDropdownItems(Arrays.asList(
            new DropdownItem("UI Scale", "custom ui scale"),
            new DropdownItem("Mod Theme"),
            new DropdownItem("Minecraft UI", "transparent minecraft ui"),
            new DropdownItem("HD Font")
        ));
        tabs.add(interfaceTab);
        
        var renderTab = new Tab("Rendering", "Render", "── Rendering Tweaks ──");
        renderTab.setDropdownItems(Arrays.asList(
            new DropdownItem("Held Item", "held item size and animations"),
            new DropdownItem("Entity Death", "skip entity death animation"),
            new DropdownItem("Entity Fire", "skip entity fire animation"),
            new DropdownItem("Lava", "clear lava"),
            new DropdownItem("Water", "clear water"),
            new DropdownItem("Vanilla XP", "xp text color"),
            new DropdownItem("Redstone", "redstone particle color"),
            new DropdownItem("IGN Colors", "custom fa colors")
        ));
        tabs.add(renderTab);
        
        var qolTab = new Tab("QoL", "QoL", "── General QoL ──");
        qolTab.setDropdownItems(Arrays.asList(
            new DropdownItem("Inventory", "overlay opacity"),
            new DropdownItem("Coordinates", "highlight coordinates"),
            new DropdownItem("Waypoints", "preset chains"),
            new DropdownItem("Ping/TPS/FPS", "debug display"),
            new DropdownItem("Copy Chat"),
            new DropdownItem("F5", "skip front perspective"),
            new DropdownItem("Keybinds", "custom keybinds"),
            new DropdownItem("Commands", "custom commands"),
            new DropdownItem("Emoji", "chat replacement"),
            new DropdownItem("Alerts", "chat alerts")
        ));
        tabs.add(qolTab);
        
        var skyblockTab = new Tab("SkyBlock", "SB", "── SkyBlock Misc ──");
        skyblockTab.setDropdownItems(Arrays.asList(
            new DropdownItem("Equipment", "equipment display"),
            new DropdownItem("Mob Health", "mob health bar"),            
            new DropdownItem("Pet Display", "include pet xp"),
            new DropdownItem("Profit", "price per item"),
            new DropdownItem("Collection", "collection progress display"),
            new DropdownItem("Diana", "track various diana stats"),
            new DropdownItem("Slayer", "track slayer xp"),
            new DropdownItem("Skill XP", "skill xp per hour"),
            new DropdownItem("Cakes", "century cake display"),
            new DropdownItem("Effects", "consumable cooldowns"),
            new DropdownItem("Cocoon", "nearby cocoon alert"),            
            new DropdownItem("Moonglade", "timer display"),
            new DropdownItem("Hyperion", "clean wither impact"),
            new DropdownItem("Sb Gui", "hide skyblock gui buttons"),
            new DropdownItem("Npc Dialogue", "accept npc dialogue"),
            new DropdownItem("Invisibug", "invisibug helper")
        ));
        tabs.add(skyblockTab);

        var audioTab = new Tab("Audio", "Audio", "── Audio ──");
        audioTab.setDropdownItems(Arrays.asList(
            new DropdownItem("Ferocity", "custom ferocity sound"),
            new DropdownItem("Mute", "mute list"),
            new DropdownItem("Catch", "custom fishing catch volume")
        ));
        tabs.add(audioTab);        
        
        var fishingTab = new Tab("Fishing", "Fish", "── Fishing ──");
        fishingTab.setDropdownItems(Arrays.asList(
            new DropdownItem("Rain", "rain warning"),
            new DropdownItem("Hotspots", "hide hotspot holograms"),
            new DropdownItem("Data", "track catch data"),
            new DropdownItem("Graph", "catch graph"),
            new DropdownItem("Chat", "rng info")
        ));
        tabs.add(fishingTab);
        
        var filterTab = new Tab("Chat Filter", "Filter", "── Chat Filter ──");
        filterTab.setDropdownItems(Arrays.asList(
            new DropdownItem("Sc Filter", "custom sea creature messages"),
            new DropdownItem("Custom", "custom filter"),
            new DropdownItem("Sack", "hide sack messages"),
            new DropdownItem("Autopet", "hide autopet messages"),
            new DropdownItem("Implosion", "hide implosion messages"),
            new DropdownItem("[Party]", "click to party"),
            new DropdownItem("Formatting", "ingame chat formatting")            
        ));
        tabs.add(filterTab);
        
        var safeguardTab = new Tab("Safeguard", "Items", "── Item Safeguard ──");
        safeguardTab.setDropdownItems(Arrays.asList(
            new DropdownItem("Protection", "sell protection"),
            new DropdownItem("Blacklist", "block auction house"),
            new DropdownItem("Slotlocking", "bind and lock slots")
        ));
        tabs.add(safeguardTab);
        
        return tabs;
    }

    private VCCategory() {
        throw new UnsupportedOperationException("Utility class");
    }
}
