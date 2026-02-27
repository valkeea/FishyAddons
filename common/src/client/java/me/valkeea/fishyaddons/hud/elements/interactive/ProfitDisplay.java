package me.valkeea.fishyaddons.hud.elements.interactive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.config.TrackerProfiles;
import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.hud.base.InteractiveHudElement;
import me.valkeea.fishyaddons.hud.core.HudButtonManager;
import me.valkeea.fishyaddons.hud.core.HudElementState;
import me.valkeea.fishyaddons.hud.core.HudUtils;
import me.valkeea.fishyaddons.tracker.PriceUtil;
import me.valkeea.fishyaddons.tracker.profit.HudDisplayCache;
import me.valkeea.fishyaddons.tracker.profit.HudDisplayCache.CachedHudData;
import me.valkeea.fishyaddons.tracker.profit.ProfitTracker;
import me.valkeea.fishyaddons.tracker.profit.TrackedItemData;
import me.valkeea.fishyaddons.ui.widget.dropdown.VCToggleMenu;
import me.valkeea.fishyaddons.ui.widget.dropdown.item.ToggleMenuItem;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class ProfitDisplay extends InteractiveHudElement {

    private static ProfitDisplay instance = null;
    private VCToggleMenu profileMenu;

    // --- Setup ---

    private ProfitDisplay() {
        super(
            Key.HUD_PROFIT_ENABLED,
            "Profit Tracker",
            20,
            40,
            12,
            0xFF29E7FF,
            false,
            true
        );
        createMenu();
        registerEvents();
    }

    public static ProfitDisplay getInstance() {
        if (instance == null) {
            instance = new ProfitDisplay();
        }
        return instance;
    }
    
    public static void refreshDisplay() {
        if (instance != null) {
            HudDisplayCache.getInstance().invalidateCache();
        }
    }

    public void createMenu() {
        this.profileMenu = new VCToggleMenu(
            this::getProfileMenuItems,
            0, - 20 * getHudSize() / 12 , 120, 12,
            ProfitDisplay::refreshDisplay
        );
        this.profileMenu.setVisible(false);
        registerToggleMenu(profileMenu);
    }

    private List<ToggleMenuItem> getProfileMenuItems() {
        List<ToggleMenuItem> items = new ArrayList<>();
        
        var profiles = TrackerProfiles.getAvailableProfiles();
        var activeProfile = TrackerProfiles.getCurrentProfile();
        
        for (String profileName : profiles) {
            items.add(new ToggleMenuItem() {
                @Override
                public String getId() {
                    return profileName;
                }

                @Override
                public String getDisplayName() {
                    return profileName;
                }

                @Override
                public boolean isEnabled() {
                    return profileName.equals(activeProfile);
                }

                @Override
                public void toggle() {
                    TrackerProfiles.setCurrentProfile(profileName);
                    refreshDisplay();
                }
            });
        }
        
        return items;
    }    

    public void registerEvents() {

        FaEvents.MOUSE_CLICK.register(event -> {
            if (mouseClicked(event.click) || instance.handleMouseClick(event.click)) {
                event.setConsumed(true);
            }
        }, EventPriority.NORMAL, EventPhase.PRE);

        FaEvents.MOUSE_SCROLL.register(event -> {
            if (handleMouseScroll(event.mouseX, event.mouseY, event.vertical)) {
                event.setConsumed(true);
            }
        }, EventPriority.LOW, EventPhase.NORMAL);
    }
    
    @Override
    protected String getMaxLinesConfigKey() {
        return Key.HUD_PROFIT_LINES;
    }
    
    @Override
    protected int getTotalLineCount() {
        return HudDisplayCache.getInstance().getSize();
    }

    // --- Display Data ---

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

    @Override
    protected List<Text> getDisplayLines(HudElementState state) {
       var data = HudDisplayCache.getInstance().getDisplayData();
       if (data.isEmpty()) return placeHolderLines();
       else return getDisplayLines(data, state.color);
    }

    private List<Text> getDisplayLines(CachedHudData displayData, int color) {

        List<Text> lines = new ArrayList<>();

        var currentProfile = TrackerProfiles.getCurrentProfile();
        var profileSuffix = "default".equals(currentProfile) ? "" : " (" + currentProfile + ")";

        int titleColor = Color.saturate(color, 0.9f);

        lines.add(Text.literal("Profit Tracker").styled(style -> style.withColor(titleColor))
            .append(Text.literal(displayData.timeString + profileSuffix)));

        var allItems = getSortedItemValues(displayData);

        IntStream.range(visibleLineIdx - 1, Math.min(visibleLineIdx - 1 + maxVisibleLines, allItems.size()))
            .forEach(i -> lines.add(getItemLine(allItems.get(i), color)));

        lines.add(
            Text.literal(("Items: ")).styled(style -> style.withColor(titleColor))
                .append(Text.literal(String.valueOf(displayData.totalItems))
                .styled(style -> style.withColor(Color.brighten(color, 0.6f)))));

        if (displayData.totalValue > 0) {
            lines.add(Text.literal("Value: ").styled(style -> style.withColor(titleColor))
                .append(Text.literal(HudUtils.formatCoins(displayData.totalValue))
                .styled(style -> style.withColor(Color.brighten(color, 0.6f)))));
        }

        return lines;
    }

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

    private Text getItemLine(ItemValueData data, int color) {

        var itemName = enhance(data.itemName);
        int quantity = data.quantity;
        var lineText = Text.literal(String.format("+%d ", quantity))
            .styled(style -> style.withColor(Color.desaturateAndDarken(color, 0.3f)))
            .append(Text.literal(itemName).styled(style -> style.withColor(Color.desaturate(color, 0.8f))));

        if (ProfitTracker.pricePerItem() && data.totalValue > 0) {
            lineText.append(Text.literal(" §7("))
                .append(Text.literal(HudUtils.formatCoins(data.totalValue)).styled(style -> style.withColor(Color.brighten(color, 0.6f))))
                .append("§7)");
        }

        return lineText;
    }

    public Set<String> getExcludedItemsForDisplay() {
        return getExcludedItems();
    }

    private Set<String> getExcludedItems() {
        var excludedStr = ItemConfig.getString(Key.EXCLUDED_ITEMS, "");
        if (excludedStr.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(excludedStr.split(",")));
    }

    // --- Rendering ---

    @Override
    public boolean shouldRender() {
        return isEditingMode() || ProfitTracker.isEnabled() && !HudDisplayCache.getInstance().getDisplayData().isEmpty();
    }

    @Override
    protected void postRenderCustom(DrawContext context, MinecraftClient mc, HudElementState state, int mouseX, int mouseY) {
        var screen = mc.currentScreen;
        if (!isEditingMode() && isInventoryOpen(mc)) {
        
            float scale = state.size / 12.0F;
            int hudX = state.x;
            int hudY = state.y;
            
            if (profileMenu.isVisible() && screen != null) {
                int buttonWidth = (int)(45 * scale);
                int buttonSpacing = (int)(2 * scale);
                int profileButtonX = hudX + 4 * (buttonWidth + buttonSpacing);
                int menuX = profileButtonX;
                int menuY = hudY;
                profileMenu.setPosition(menuX, menuY);
                profileMenu.render(context, screen, mouseX, mouseY, scale);
            }

        } else if (screen != null && profileMenu.isVisible()) {
            profileMenu.setVisible(false);
        }
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        if (profileMenu.isVisible()) return true;
        return super.isHovered(mouseX, mouseY);
    }

    // --- Interaction ---

    private boolean handleMouseScroll(double mouseX, double mouseY, double scrollAmount) {
        if (handleMenuScroll(mouseX, mouseY, scrollAmount)) return true;
        if (isInventoryOpen(MinecraftClient.getInstance()) && shouldRender()) {
            return handleLineScroll(mouseX, mouseY, scrollAmount);
        }
        return false;
    }    

    @Override
    protected void setupButtons(HudButtonManager manager, HudElementState state) {
        manager.addButton("Refresh", btn ->  handleButtonClick(0));
        manager.addButton("Save", btn -> handleButtonClick(1));
        manager.addButton("Delete", btn -> handleButtonClick(2));
        manager.addButton("Sack", btn -> handleButtonClick(3));
        manager.addButton("Profile", btn -> handleButtonClick(4));
    }

    @Override
    protected boolean isLineClickable(int lineIndex, Text line) {
        return line.getString().startsWith("+");
    }
    
    @Override
    protected List<Text> getLineTooltip(int lineIndex, Text line) {
        if (isLineClickable(lineIndex, line) && line.getString().startsWith("+")) {
            return List.of(
                Text.literal("[Click to hide from tracking]"),
                Text.literal("§7Item value will still be counted towards total profit."),
                Text.literal("§7Use §3/fp ignored §7to view or restore ignored items.")
            );
            }
        return List.of();
    }

    @Override
    protected List<Text> getTooltipForLine(int lineIndex, Text line) {
        if (lineIndex >= maxVisibleLines) {
            int hiddenCount = getTotalLineCount() - maxVisibleLines;
            if (hiddenCount > 0) {
                return List.of(Text.literal("§bScroll§7 to view hidden items (" + hiddenCount + " hidden)"));
            }
        }
        return super.getTooltipForLine(lineIndex, line);
    }

    @Override
    protected void handleLineClick(Text line) {

        var name = line.getString().replaceAll("§.", "");
        int nameStart = name.indexOf(' ') + 1;
        int nameEnd = name.indexOf(" (");

        if (nameEnd == -1) nameEnd = name.length();

        var itemName = name.substring(nameStart, nameEnd);
        if (itemName.isEmpty()) return;

        var displayName = enhance(itemName);
        addExcludedItem(itemName.toLowerCase());
        FishyNotis.send(Text.literal("§bHiding item from tracker: §b" + displayName
        + " §b[§3/fp restore§b]"));
    }

    private boolean mouseClicked(Click click) {
        var mc = MinecraftClient.getInstance();
        if (!isInventoryOpen(mc) || !shouldRender()) return false;

        float scale = getCachedState().size / 12.0F;
        if (profileMenu.isVisible() && profileMenu.mouseClicked(click, false, scale)) {
            return true;
        } else if (profileMenu.isVisible()) {
            profileMenu.setVisible(false);
        }
        
        return false;
    }
    
    private void handleButtonClick(int buttonIndex) {
        boolean changed = false;
        switch (buttonIndex) {
            case 0:
                FishyNotis.notice("§bRefreshing cache...");
                PriceUtil.refreshPrices();
                changed = true;
                break;
            case 1:
                ProfitTracker.save();
                break;
            case 2:
                String profileName = TrackerProfiles.getCurrentProfile();
                if ("default".equals(profileName)) {
                    TrackedItemData.clearAll();
                    changed = true;
                } else if (TrackerProfiles.deleteProfile(profileName)) {
                    ProfitTracker.onDelete(profileName);
                    changed = true;
                } else {
                    FishyNotis.notice("§7No file to delete");
                }
                break;
            case 3:
                boolean state = FishyConfig.getState(Key.TRACK_SACK, false);
                FishyConfig.setState(Key.TRACK_SACK, false);
                PriceUtil.refresh();
                if (state) FishyNotis.on("Sack tracking");
                else FishyNotis.off("Sack tracking");
                break;
            case 4:
                profileMenu.setVisible(!profileMenu.isVisible());
                break;
            default:
                break;    
        }

        if (changed) refreshDisplay();
    }

    // --- Utilities ---
    
    private void addExcludedItem(String itemName) {
        var excluded = getExcludedItems();
        excluded.add(itemName);
        saveExcludedItems(excluded);
    }
    
    private void removeExcludedItem(String itemName) {
        var excluded = getExcludedItems();
        excluded.remove(itemName);
        saveExcludedItems(excluded);
    }
    
    private void saveExcludedItems(Set<String> excluded) {
        var excludedStr = String.join(",", excluded);
        ItemConfig.setString(Key.EXCLUDED_ITEMS, excludedStr);
        refreshDisplay();
    }

    public void restoreExcludedItem(String itemName) {
        removeExcludedItem(itemName);
        var displayName = enhance(itemName);
        FishyNotis.send(Text.literal("§aRestored item: §f" + displayName));
    }
    
    public void restoreAllExcludedItems() {
        var excluded = getExcludedItems();
        if (excluded.isEmpty()) {
            FishyNotis.notice("§7No excluded items to restore");
            return;
        }
        
        saveExcludedItems(new HashSet<>());
        FishyNotis.send("§aRestored all excluded items");
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

    private List<Text> placeHolderLines() {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("Profit Tracker (5m)"));
        lines.add(Text.literal("§b+3 §fRecombobulator 3000"));
        lines.add(Text.literal("Items: §3"));
        lines.add(Text.literal("Value: §b~27m"));
        return lines;
    }
}
