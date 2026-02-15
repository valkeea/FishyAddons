package me.valkeea.fishyaddons.hud.elements.custom;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.config.TrackerProfiles;
import me.valkeea.fishyaddons.hud.core.HudDisplayCache;
import me.valkeea.fishyaddons.hud.core.HudDisplayCache.CachedHudData;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.hud.core.HudElement;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.tracker.profit.ItemTrackerData;
import me.valkeea.fishyaddons.tracker.profit.SackDropParser;
import me.valkeea.fishyaddons.tracker.profit.ProfitTracker;
import me.valkeea.fishyaddons.ui.widget.VCButton;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class TrackerDisplay implements HudElement {
    private static final String HUD_KEY = me.valkeea.fishyaddons.config.Key.HUD_TRACKER_ENABLED;
    private static TrackerDisplay instance = null;

    private HudElementState cachedState = null;
    private java.util.List<ItemValueData> lastDisplayedItems = new java.util.ArrayList<>();
    private int lastHudX;
    private int lastHudY;
    private int lastHudSize;
    private float lastScale;
    private boolean editingMode = false;

    private TrackerDisplay() {}
    public static TrackerDisplay getInstance() {
        if (instance == null) {
            instance = new TrackerDisplay();
        }
        return instance;
    }
    
    public static void refreshDisplay() {
        if (instance != null) {
            HudDisplayCache.getInstance().invalidateCache();
        }
    }

    private boolean isTrackerVisible() {
        return !HudDisplayCache.getInstance().getDisplayData().isEmpty() &&
                ProfitTracker.isEnabled();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!editingMode && !ProfitTracker.isEnabled()) return;

        var mc = MinecraftClient.getInstance();
        var state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        int color = state.color;
        boolean showBg = state.bg;
        float scale = size / 12.0F;
        var displayData = HudDisplayCache.getInstance().getDisplayData();

        if (!editingMode && displayData.isEmpty()) return;

        List<Text> lines = getDisplayLines(editingMode, displayData, color);

        int maxWidth = getMaxLineWidth(mc, lines);
        int totalHeight = lines.size() * size;
        int scaledWidth = (int)(maxWidth * scale);
        int scaledHeight = (int)(totalHeight * scale);       

        if (showBg) {
            drawBackground(context, hudX, hudY, scaledWidth, scaledHeight);
        }

        drawTextLines(context, lines, hudX, hudY, scale, size, color);

        lastHudX = hudX;
        lastHudY = hudY;
        lastHudSize = size;
        lastScale = scale;
        
        lastDisplayedItems.clear();
        if (!editingMode && !displayData.isEmpty()) {
            List<ItemValueData> allItems = getSortedItemValues(displayData);
            allItems.stream()
                .limit(15)
                .forEach(lastDisplayedItems::add);
        }

        if (!editingMode && isInventoryOpen(mc)) {
            int buttonY = Math.max(10, hudY - 30);
            drawButtons(context, hudX, buttonY);

        }
    }

    // Sort and format item values
    private List<ItemValueData> getSortedItemValues(CachedHudData displayData) {

        List<ItemValueData> itemsWithValue = new ArrayList<>();
        List<ItemValueData> itemsWithoutValue = new ArrayList<>();

        Set<String> excludedItems = getExcludedItems();

        boolean useMinValueFilter = FishyConfig.getState(Key.VALUE_FILTER, false);
        float minItemValue = FishyConfig.getFloat(Key.FILTER_MIN_VALUE, 0);

        displayData.items.entrySet().forEach(entry -> {
            String itemName = entry.getKey();
            int quantity = entry.getValue();
            double unitPrice = displayData.itemValues.getOrDefault(itemName, 0.0);
            double totalValue = unitPrice * quantity;
            
            if (excludedItems.contains(itemName)) return;
            if (useMinValueFilter && unitPrice > 0 && unitPrice < minItemValue) return;

            var data = new ItemValueData(itemName, quantity, totalValue);

            if (totalValue > 0) itemsWithValue.add(data);
            else itemsWithoutValue.add(data);
        });

        itemsWithValue.sort((a, b) -> Double.compare(b.totalValue, a.totalValue));
        itemsWithoutValue.sort((a, b) -> Integer.compare(b.quantity, a.quantity));

        List<ItemValueData> allItems = new ArrayList<>();

        allItems.addAll(itemsWithValue);
        allItems.addAll(itemsWithoutValue);
        return allItems;
    }

    // Tracker lines and formatting 
    private List<Text> getDisplayLines(boolean editingMode, CachedHudData displayData, int color) {

        List<Text> lines = new ArrayList<>();

        if (editingMode) {
            lines.add(Text.literal("Profit Tracker (5m)"));
            lines.add(Text.literal("§b+3 §fRecombobulator 3000"));
            lines.add(Text.literal("Items: §3"));
            lines.add(Text.literal("Value: §b~27m"));

        } else if (!displayData.isEmpty()) {
            String currentProfile = TrackerProfiles.getCurrentProfile();
            String profileSuffix = "default".equals(currentProfile) ? "" : " (" + currentProfile + ")";

            int themeColor = FishyMode.getCmdColor();
            lines.add(Text.literal("Profit Tracker").styled(style -> style.withColor(themeColor))
                .append(Text.literal(displayData.timeString + profileSuffix)));

            List<ItemValueData> allItems = getSortedItemValues(displayData);

            allItems.stream()
                .limit(15)
                .forEach(data -> lines.add(getItemLine(data, color)));
            lines.add(
                Text.literal(("Items: ")).styled(style -> style.withColor(themeColor).withBold(true))
                    .append(Text.literal(String.valueOf(displayData.totalItems))
                    .styled(style -> style.withColor(Color.brighten(color, 0.6f)))));

            if (displayData.totalValue > 0) {
                lines.add(Text.literal("Value: ").styled(style -> style.withColor(themeColor).withBold(true))
                    .append(Text.literal(formatCoins(displayData.totalValue))
                    .styled(style -> style.withColor(Color.brighten(color, 0.6f)))));
            }

        } else {
            lines.add(Text.literal("No items tracked"));
        }

        return lines;
    }

    // Format item line for display
    private Text getItemLine(ItemValueData data, int color) {

        String itemName = enhance(data.itemName);
        int quantity = data.quantity;
        MutableText lineText = Text.literal(String.format("+%d ", quantity)).styled(style -> style.withColor(color))
            .append(Text.literal(itemName).styled(style -> style.withColor(Color.desaturate(color, 0.8f))));

        if (ProfitTracker.isOn() && data.totalValue > 0) {
            lineText.append(Text.literal(" §7("))
                .append(Text.literal(formatCoins(data.totalValue)).styled(style -> style.withColor(Color.brighten(color, 0.6f))))
                .append("§7)");
        }

        return lineText;
    }

    private int getMaxLineWidth(MinecraftClient mc, List<Text> lines) {
        int maxWidth = 0;
        for (Text line : lines) {
            int lineWidth = mc.textRenderer.getWidth(line);
            maxWidth = Math.max(maxWidth, lineWidth);
        }
        return maxWidth;
    }

    private void drawBackground(DrawContext context, int hudX, int hudY, int scaledWidth, int scaledHeight) {
        int bgX1 = hudX - 2;
        int bgY1 = hudY - 2;
        int bgX2 = hudX + scaledWidth + 4;
        int bgY2 = hudY + scaledHeight + 2;
        context.fill(bgX1, bgY1, bgX2, bgY2, 0x80000000);
    }

    private void drawTextLines(DrawContext context, List<Text> lines, int hudX, int hudY, float scale, int size, int color) {

        var mc = MinecraftClient.getInstance();

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(hudX, hudY);
        context.getMatrices().scale(scale, scale);

        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            int yOffset = i * size;
            context.drawText(mc.textRenderer, line, 0, yOffset, color, false);
        }

        context.getMatrices().popMatrix();
    }
    
    private boolean isInventoryOpen(MinecraftClient mc) {
        if (mc.currentScreen == null) {
            return false;
        }
        
        String screenClassName = mc.currentScreen.getClass().getSimpleName();
        return screenClassName.equals("class_490") ||
               screenClassName.equals("class_476") ||
               screenClassName.equals("class_475") ||
               screenClassName.contains("Inventory");
    }
    
    private void drawButtons(DrawContext context, int x, int y) {

        String[] buttonTexts = {"Refresh", "Save", "Delete", "Sack", "Profile"};
        float scale = getCachedState().size / 12.0F;
        int buttonWidth = (int)(40 * scale);
        int buttonHeight = (int)(16 * scale);
        int buttonSpacing = (int)(2 * scale);
        
        var mc = MinecraftClient.getInstance();
        double actualMouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double actualMouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        
        for (int i = 0; i < buttonTexts.length; i++) {
            int buttonX = x + i * (buttonWidth + buttonSpacing);
            int buttonY = y;
            
            boolean isHovered = VCButton.isHovered(buttonX, buttonY, buttonWidth, buttonHeight, (int)actualMouseX, (int)actualMouseY);
            
            VCButton.render(context, mc.textRenderer,
                VCButton.standard(buttonX, buttonY, buttonWidth, buttonHeight, buttonTexts[i])
                    .withHovered(isHovered)
                    .withScale(0.5f)
            );                   
        }
    }

    public boolean handleMouseClick(double mouseX, double mouseY, int button) {

        var mc = MinecraftClient.getInstance();
        if (!isInventoryOpen(mc) || !isTrackerVisible()) return false;
        
        var state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        float scale = getCachedState().size / 12.0F;
        int buttonY = Math.max(10, hudY - 30);
        int buttonWidth = (int)(40 * scale);
        int buttonHeight = (int)(16 * scale);
        int buttonSpacing = (int)(2 * scale);

        for (int i = 0; i < 5; i++) {
            int buttonX = hudX + i * (buttonWidth + buttonSpacing);
            
            if (VCButton.isHovered(buttonX, buttonY, buttonWidth, buttonHeight, (int)mouseX, (int)mouseY)) {
                if (i == 4 && button == 1) {
                    showProfileMenu();
                } else if (button == 0) {
                    handleButtonClick(i);
                }
                return true;
            }
        }

        if (button == 1 && !lastDisplayedItems.isEmpty()) {
            return handleItemLineRightClick(mouseX, mouseY);
        }

        return false;
    }
    
    private boolean handleItemLineRightClick(double mouseX, double mouseY) {
        int lineHeight = (int)(lastHudSize * lastScale);
        int startY = lastHudY + lineHeight;
        
        for (int i = 0; i < lastDisplayedItems.size(); i++) {
            int lineY = startY + (i * lineHeight);
            int nextLineY = lineY + lineHeight;
            
            if (mouseY >= lineY && mouseY < nextLineY && 
                mouseX >= lastHudX && mouseX <= lastHudX + 200) {

                var clickedItem = lastDisplayedItems.get(i);
                addExcludedItem(clickedItem.itemName);

                String displayName = enhance(clickedItem.itemName);
                FishyNotis.send(Text.literal("§bFishyAddons §dwill no longer track: §f" + displayName));
                FishyNotis.alert(Text.literal("§       §7Use §3/fp ignored §7to see or restore ignored items"));
                
                return true;
            }
        }
        
        return false;
    }
    
    public void restoreExcludedItem(String itemName) {
        removeExcludedItem(itemName);
        String displayName = enhance(itemName);
        FishyNotis.send(Text.literal("§aRestored item: §f" + displayName));
    }
    
    public void restoreAllExcludedItems() {
        Set<String> excluded = getExcludedItems();
        if (excluded.isEmpty()) {
            FishyNotis.notice("§7No excluded items to restore");
            return;
        }
        
        saveExcludedItems(new java.util.HashSet<>());
        FishyNotis.send("§aRestored all excluded items");
    }
    
    public Set<String> getExcludedItemsForDisplay() {
        return getExcludedItems();
    }
    
    private void handleButtonClick(int buttonIndex) {
        switch (buttonIndex) {
            case 0:
                FishyNotis.notice("§bRefreshing cache...");
                ItemTrackerData.forceRefreshAuctionCache();
                ItemTrackerData.refreshBazaarPrices();
                HudDisplayCache.getInstance().invalidateCache();
                break;
            case 1:
                ProfitTracker.save();
                break;
            case 2:
                String profileName = TrackerProfiles.getCurrentProfile();
                if ("default".equals(profileName)) {
                    FishyNotis.warn("§cCannot delete the default profile!");
                    FishyNotis.alert(Text.literal("§7Use §3/fp clear §7if you wish to reset the session."));
                } else if (TrackerProfiles.deleteProfile(profileName)) {
                    ProfitTracker.onDelete(profileName);
                } else {
                    FishyNotis.notice("§7No file to delete");
                }
                break;
            case 3:
                if (SackDropParser.isOn()) {
                    SackDropParser.toggle();
                    FishyNotis.off("Sack tracking");
                } else {
                    SackDropParser.toggle();
                    FishyNotis.on("Sack tracking");
                }
                break;
            case 4:
                cycleProfile();
                break;
            default:
                break;
        }
    }
    
    private void cycleProfile() {
        List<String> profiles = TrackerProfiles.getAvailableProfiles();
        String currentProfile = TrackerProfiles.getCurrentProfile();
        
        int currentIndex = profiles.indexOf(currentProfile);
        int nextIndex = (currentIndex + 1) % profiles.size();
        String nextProfile = profiles.get(nextIndex);

        TrackerProfiles.setCurrentProfile(nextProfile);
        FishyNotis.send(Text.literal("§dSwitched to profile: §3" + nextProfile));
        HudDisplayCache.getInstance().invalidateCache();
    }
    
    private void showProfileMenu() {
        FishyNotis.alert(Text.literal("§bYou can also left-click to cycle profiles!"));        
        me.valkeea.fishyaddons.command.handler.FpRoot.sendProfileClickable();
    }
    
    private String enhance(String itemName) {
        if (itemName == null || itemName.isEmpty()) return itemName;

        if (itemName.startsWith("ultimate_")) {
            itemName = itemName.replaceFirst("ultimate_", "");
        }
        
        String[] words = itemName.split(" ");
        var result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            String word = words[i];

            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));

                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        
        return result.toString();
    }

    private String formatCoins(double coins) {
        if (coins >= 1_000_000.0) {
            return String.format("%.1fm", coins / 1_000_000.0);
        } else if (coins >= 1_000.0) {
            return String.format("%.1fk", coins / 1_000.0);
        } else {
            return String.format("%.0f", coins);
        }
    }

    @Override
    public Rectangle getBounds(MinecraftClient mc) {

        var state = getCachedState();
        float scale = state.size / 12.0F;
        int estimatedWidth = (int)(150 * scale);
        int estimatedHeight = (int)(9 * state.size * scale);
        
        return new Rectangle(state.x, state.y, estimatedWidth, estimatedHeight);
    }

    @Override
    public HudElementState getCachedState() {
        if (cachedState == null) {
            cachedState = new HudElementState(
                getHudX(), getHudY(), getHudSize(),
                getHudColor(), getHudOutline(), getHudBg()
            );
        }
        return cachedState;
    }

    @Override
    public void invalidateCache() {
        cachedState = null;
    }

    private java.util.Set<String> getExcludedItems() {
        String excludedStr = ItemConfig.getString(Key.EXCLUDED_ITEMS, "");
        if (excludedStr.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(excludedStr.split(",")));
    }
    
    private void addExcludedItem(String itemName) {
        Set<String> excluded = getExcludedItems();
        excluded.add(itemName);
        saveExcludedItems(excluded);
    }
    
    private void removeExcludedItem(String itemName) {
        java.util.Set<String> excluded = getExcludedItems();
        excluded.remove(itemName);
        saveExcludedItems(excluded);
    }
    
    private void saveExcludedItems(java.util.Set<String> excluded) {
        String excludedStr = String.join(",", excluded);
        ItemConfig.setString(Key.EXCLUDED_ITEMS, excludedStr);
        HudDisplayCache.getInstance().invalidateCache();
    }

    @Override public int getHudX() { return FishyConfig.getHudX(HUD_KEY, 14); }
    @Override public int getHudY() { return FishyConfig.getHudY(HUD_KEY, 100); }
    @Override public void setHudPosition(int x, int y) { FishyConfig.setHudX(HUD_KEY, x); FishyConfig.setHudY(HUD_KEY, y); }
    @Override public int getHudSize() { return FishyConfig.getHudSize(HUD_KEY, 12); }
    @Override public void setHudSize(int size) { FishyConfig.setHudSize(HUD_KEY, size); }
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_KEY, 0x8AE2B6); }
    @Override public void setHudColor(int color) { FishyConfig.setHudColor(HUD_KEY, color); }
    @Override public boolean getHudOutline() { return false; }
    @Override public void setHudOutline(boolean outline) { /*none*/ }   
    @Override public boolean getHudBg() { return FishyConfig.getHudBg(HUD_KEY, true); }
    @Override public void setHudBg(boolean bg) { FishyConfig.setHudBg(HUD_KEY, bg); }
    @Override public void setEditingMode(boolean editing) { this.editingMode = editing; }
    @Override public String getDisplayName() { return "Profit Tracker"; }    
    
    private static class ItemValueData {
        final String itemName;
        final int quantity;
        final double totalValue;
        
        ItemValueData(String itemName, int quantity, double totalValue) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.totalValue = totalValue;
        }
    }
}
