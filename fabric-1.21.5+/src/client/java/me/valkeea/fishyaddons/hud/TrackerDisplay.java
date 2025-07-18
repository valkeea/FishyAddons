package me.valkeea.fishyaddons.hud;

import java.awt.Rectangle;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.hud.HudDisplayCache.CachedHudData;
import me.valkeea.fishyaddons.tracker.ItemTrackerData;
import me.valkeea.fishyaddons.tracker.SackDropParser;
import me.valkeea.fishyaddons.tracker.TrackerUtils;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TrackerDisplay implements HudElement {
    private boolean editingMode = false;
    private static final String HUD_KEY = me.valkeea.fishyaddons.config.Key.HUD_TRACKER_ENABLED;
    private HudElementState cachedState = null;
    private static TrackerDisplay instance = null;
    public TrackerDisplay() { instance = this; }
    public static TrackerDisplay getInstance() {
        return instance;
    }
    
    public static void refreshDisplay() {
        if (instance != null) {
            HudDisplayCache.getInstance().invalidateCache();
        }
    }
    
    public void register() {
        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
            layeredDrawer.attachLayerAfter(
                IdentifiedLayer.MISC_OVERLAYS,
                Identifier.of("fishyaddons", "profit_tracker_hud"),
                (context, tickCounter) -> render(context, 0, 0)
            )
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY) {
        if (!editingMode && !FishyConfig.getState(HUD_KEY, false)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        HudElementState state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int size = state.size;
        int color = state.color;
        boolean showBg = state.bg;
        float scale = size / 12.0F;
        CachedHudData displayData = HudDisplayCache.getInstance().getDisplayData();

        if (!editingMode && displayData.isEmpty()) return;

        java.util.List<Text> lines = getDisplayLines(editingMode, displayData);

        int maxWidth = getMaxLineWidth(mc, lines);
        int totalHeight = lines.size() * size;
        int scaledWidth = (int)(maxWidth * scale);
        int scaledHeight = (int)(totalHeight * scale);

        if (editingMode || showBg) {
            drawBackground(context, hudX, hudY, scaledWidth, scaledHeight);
        }

        drawTextLines(context, mc, lines, hudX, hudY, scale, size, color);

        if (!editingMode && isInventoryOpen(mc)) {
            me.valkeea.fishyaddons.render.FaLayers.renderAtTopLevel(context, () -> {
                int buttonY = Math.max(10, hudY - 30);
                drawButtons(context, hudX, buttonY, mouseX, mouseY);
            });
        }

        if (editingMode) {
            context.drawBorder(hudX - 1, hudY - 1, scaledWidth + 2, scaledHeight + 2, 0xFFFFFFFF);
        }
    }

    // Sort and format item values
    private java.util.List<ItemValueData> getSortedItemValues(CachedHudData displayData) {
        java.util.List<ItemValueData> itemsWithValue = new java.util.ArrayList<>();
        java.util.List<ItemValueData> itemsWithoutValue = new java.util.ArrayList<>();
        boolean useMinValueFilter = FishyConfig.getState("minValueFilter", false);
        float minItemValue = FishyConfig.getFloat("minItemValue", 0);

        displayData.items.entrySet().forEach(entry -> {
            String itemName = entry.getKey();
            int quantity = entry.getValue();
            double unitPrice = displayData.itemValues.getOrDefault(itemName, 0.0);
            double totalValue = unitPrice * quantity;
            if (useMinValueFilter && unitPrice > 0 && unitPrice < minItemValue) return;
            ItemValueData data = new ItemValueData(itemName, quantity, totalValue);
            if (totalValue > 0) itemsWithValue.add(data);
            else itemsWithoutValue.add(data);
        });

        itemsWithValue.sort((a, b) -> Double.compare(b.totalValue, a.totalValue));
        itemsWithoutValue.sort((a, b) -> Integer.compare(b.quantity, a.quantity));
        java.util.List<ItemValueData> allItems = new java.util.ArrayList<>();
        allItems.addAll(itemsWithValue);
        allItems.addAll(itemsWithoutValue);
        return allItems;
    }

    // Tracker lines and formatting 
    private java.util.List<Text> getDisplayLines(boolean editingMode, CachedHudData displayData) {
        java.util.List<Text> lines = new java.util.ArrayList<>();
        if (editingMode) {
            lines.add(Text.literal("Profit Tracker (5m)"));
            lines.add(Text.literal("§b+3 §fRecombobulator 3000"));
            lines.add(Text.literal("Total: §3 items"));
            lines.add(Text.literal("Value: §b~27m coins"));
        } else if (!displayData.isEmpty()) {
            lines.add(Text.literal("Profit Tracker" + displayData.timeString));
            java.util.List<ItemValueData> allItems = getSortedItemValues(displayData);
            allItems.stream()
                .limit(15)
                .forEach(data -> lines.add(Text.literal(getItemLine(data))));
            lines.add(Text.literal(String.format("Total: §3%d items", displayData.totalItems)));
            if (displayData.totalValue > 0) {
                lines.add(Text.literal(String.format("Value: §b%s%s", displayData.formattedValue, displayData.apiIndicator)));
            }
        } else {
            lines.add(Text.literal("No items tracked"));
        }
        return lines;
    }

    // Format item line for display
    private String getItemLine(ItemValueData data) {
        String itemName = capitalizeItemName(data.itemName);
        int quantity = data.quantity;
        StringBuilder lineBuilder = new StringBuilder();
        lineBuilder.append(String.format("§3+%d §7%s", quantity, itemName));
        if (TrackerUtils.isOn() && data.totalValue > 0) {
            lineBuilder.append(String.format(" §7(§b%s§7)", formatCoins(data.totalValue)));
        }
        return lineBuilder.toString();
    }

    private int getMaxLineWidth(MinecraftClient mc, java.util.List<Text> lines) {
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

    private void drawTextLines(DrawContext context, MinecraftClient mc, java.util.List<Text> lines, int hudX, int hudY, float scale, int size, int color) {
        context.getMatrices().push();
        context.getMatrices().translate(hudX, hudY, 0);
        context.getMatrices().scale(scale, scale, 1.0F);
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            int yOffset = i * size;
            context.drawText(mc.textRenderer, line, 0, yOffset, color, true);
        }
        context.getMatrices().pop();
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
    
    // Inventory buttons
    private void drawButtons(DrawContext context, int x, int y, int mouseX, int mouseY) {
        String[] buttonTexts = {"Refresh", "Save", "Delete", "Sack"};
        int buttonWidth = 40;
        int buttonHeight = 16;
        int buttonSpacing = 2;
        
        for (int i = 0; i < buttonTexts.length; i++) {
            int buttonX = x + i * (buttonWidth + buttonSpacing);
            int buttonY = y;
            
            boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                              mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
            
            int bgColor = isHovered ? 0x80FFFFFF : 0x80000000;
            context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, bgColor);
            context.drawBorder(buttonX, buttonY, buttonWidth, buttonHeight, 0xFFFFFFFF);
            MinecraftClient mc = MinecraftClient.getInstance();
            int textColor = isHovered ? 0x000000 : 0xFFFFFF;
            int textX = buttonX + (buttonWidth - mc.textRenderer.getWidth(buttonTexts[i])) / 2;
            int textY = buttonY + (buttonHeight - mc.textRenderer.fontHeight) / 2;
            context.drawText(mc.textRenderer, buttonTexts[i], textX, textY, textColor, false);
        }
    }

    // Register mouse clicks for buttons
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!isInventoryOpen(mc)) return false;
        
        HudElementState state = getCachedState();
        int hudX = state.x;
        int hudY = state.y;
        int buttonY = Math.max(10, hudY - 30); // Use the same calculation as in drawButtons
        int buttonWidth = 40;
        int buttonHeight = 16;
        int buttonSpacing = 2;
        
        for (int i = 0; i < 4; i++) {
            int buttonX = hudX + i * (buttonWidth + buttonSpacing);
            
            if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth &&
                mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                
                handleButtonClick(i);
                return true;
            }
        }

        return false;
    }
    
    private void handleButtonClick(int buttonIndex) {
        switch (buttonIndex) {
            case 0:
                FishyNotis.send(Text.literal("§bRefreshing cache..."));
                ItemTrackerData.forceRefreshAuctionCache();
                HudDisplayCache.getInstance().invalidateCache();
                break;
            case 1:
                ItemTrackerData.saveToJson();
                FishyNotis.send(Text.literal("§aSaved tracker data to file"));
                break;
            case 2:
                if (ItemTrackerData.deleteJsonFile()) {
                    FishyNotis.send(Text.literal("§cDeleted tracker data file"));
                } else {
                    FishyNotis.send(Text.literal("§7No file to delete"));
                }
                break;
            case 3:
                if (SackDropParser.isOn()) {
                    SackDropParser.toggle();
                    FishyNotis.send(Text.literal("§cSack tracking disabled"));
                } else {
                    SackDropParser.toggle();
                    FishyNotis.send(Text.literal("§aSack tracking enabled"));
                }
                break;
            default:
                break;
        }
    }
    
    private String capitalizeItemName(String itemName) {
        if (itemName == null || itemName.isEmpty()) return itemName;
        
        String[] words = itemName.split(" ");
        StringBuilder result = new StringBuilder();
        
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
        HudElementState state = getCachedState();
        float scale = state.size / 12.0F;

        // Estimate
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

    @Override public int getHudX() { return FishyConfig.getHudX(HUD_KEY, 14); }
    @Override public int getHudY() { return FishyConfig.getHudY(HUD_KEY, 100); }
    @Override public void setHudPosition(int x, int y) { FishyConfig.setHudX(HUD_KEY, x); FishyConfig.setHudY(HUD_KEY, y); }
    @Override public int getHudSize() { return FishyConfig.getHudSize(HUD_KEY, 12); }
    @Override public void setHudSize(int size) { FishyConfig.setHudSize(HUD_KEY, size); }
    @Override public int getHudColor() { return FishyConfig.getHudColor(HUD_KEY, 0x8AE2B6); }
    @Override public void setHudColor(int color) { FishyConfig.setHudColor(HUD_KEY, color); }
    @Override public boolean getHudOutline() { return FishyConfig.getHudOutline(HUD_KEY, false); }
    @Override public void setHudOutline(boolean outline) { /*none*/ }   
    @Override public boolean getHudBg() { return FishyConfig.getHudBg(HUD_KEY, true); }
    @Override public void setHudBg(boolean bg) { FishyConfig.setHudBg(HUD_KEY, bg); }
    @Override public void setEditingMode(boolean editing) { this.editingMode = editing; }
    @Override public String getDisplayName() { return "Profit Tracker"; }


    /**
     * Helper class for sorting items by total value
     */
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
