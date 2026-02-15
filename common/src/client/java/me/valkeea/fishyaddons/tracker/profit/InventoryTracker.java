package me.valkeea.fishyaddons.tracker.profit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.ItemData;
import me.valkeea.fishyaddons.util.text.FromText;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class InventoryTracker {

    private static final int STACK_INCREASE_THRESHOLD = 32;
    private static final long DROP_CORRELATION_WINDOW = 2000;
    private static final long MONITORING_WINDOW = 180000;
    private static final long LS_WINDOW = 1000;

    private static final String ULTIMATE_PREFIX = "ultimate_";
    private static final String CLEAN_REGEX = "§[0-9a-fk-or]";

    private static final List<String> TRACKED_PLAYER_HEADS = new ArrayList<>();
    private static final List<String> TRACKED_GHAST_TEARS = new ArrayList<>();
    private static final List<String> TRACKED_TOOLS = new ArrayList<>();    

    private static boolean monitoringEnabled = false;
    private static boolean lsEnabled = false;

    private static long monitoringStartTime = 0;
    private static long lsStartTime = 0;    
    
    static {
        TRACKED_PLAYER_HEADS.add("emperor's skull");
        TRACKED_PLAYER_HEADS.add("magma lord fragment");
        TRACKED_PLAYER_HEADS.add("soul fragment");
        TRACKED_PLAYER_HEADS.add("foraging exp boost");
        TRACKED_PLAYER_HEADS.add("minos relic");
        TRACKED_PLAYER_HEADS.add("dwarf turtle shelmet");
        TRACKED_PLAYER_HEADS.add("antique remedies");
        TRACKED_PLAYER_HEADS.add("crown of greed");
        TRACKED_PLAYER_HEADS.add("water hydra head");
        TRACKED_GHAST_TEARS.add("great white shark tooth");
        TRACKED_TOOLS.add("fishing exp boost");
        TRACKED_TOOLS.add("foraging exp boost");        
    }
    
    private static final Map<Long, String> recentTrackedItemDrops = new ConcurrentHashMap<>();
    private static final Map<String, Integer> lastKnownStackSizes = new ConcurrentHashMap<>();

    private InventoryTracker() {}
    
    public static void onItemAdded(ItemStack stack) {
        if (stack.isEmpty()) return;
        
        if (!isMonitoringActive()) {
            return;
        }

        var newItem = stack.getItem();
        
        if (newItem == Items.PLAYER_HEAD) {
            handlePlayerHeadAdded(stack);
            return;
        }

        if (newItem == Items.ENCHANTED_BOOK) {
            handleBookAdded(stack);
            return;
        }

        if (newItem == Items.GHAST_TEAR) {
            handleGhastTearAdded(stack);
            return;
        }

        if (newItem == Items.IRON_AXE || newItem == Items.COD) {
            handleToolAdded(stack);
        }
    }   
    
    private static boolean isMonitoringActive() {
        if (!monitoringEnabled) return false;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - monitoringStartTime > MONITORING_WINDOW) {          
            monitoringEnabled = false;
            monitoringStartTime = 0;
            return false;
        }
        return true;
    }

    public static boolean isLsMonitoringActive() {
        if (!lsEnabled) return false;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lsStartTime > LS_WINDOW) {
            lsEnabled = false;
            return false;
        }
        return true;
    }
    
    private static void handlePlayerHeadAdded(ItemStack stack) {
        var displayName = stack.getName();
        var cleanName = displayName.getString().toLowerCase().replaceAll(CLEAN_REGEX, "").trim();

        if (!TRACKED_PLAYER_HEADS.contains(cleanName)) {
            return;
        }
        
        int currentStackSize = stack.getCount();
        int previousStackSize = lastKnownStackSizes.getOrDefault(cleanName, 0);
        
        if (currentStackSize > previousStackSize) {
            int rawIncrease = currentStackSize - previousStackSize;
            int newItems = (rawIncrease > STACK_INCREASE_THRESHOLD) ? 1 : rawIncrease;
            
            lastKnownStackSizes.put(cleanName, currentStackSize);
            
            String uuid = extractUUID(stack);
            var dropResult = ItemTrackerData.addDrop(cleanName, newItems, null, uuid);
            if (dropResult.shouldNotify) FishyNotis.trackerNoti(displayName, newItems);

        } else lastKnownStackSizes.put(cleanName, currentStackSize);
    }

    private static void handleGhastTearAdded(ItemStack stack) {
        var displayName = stack.getName();
        var cleanName = displayName.getString().toLowerCase().replaceAll(CLEAN_REGEX, "").trim();

        if (TRACKED_GHAST_TEARS.contains(cleanName)) {
            String uuid = extractUUID(stack);
            var dropResult = ItemTrackerData.addDrop(cleanName, 1, null, uuid);
            
            if (dropResult.shouldNotify) {
                FishyNotis.trackerNoti(displayName, 1);
            }
        }
    }

    private static void handleToolAdded(ItemStack stack) {
        var displayName = stack.getName();
        var cleanName = displayName.getString().toLowerCase().replaceAll(CLEAN_REGEX, "").trim();      

        if (TRACKED_TOOLS.contains(cleanName)) {
            var rarity = getRarityTier(displayName.getSiblings().get(0).getStyle());
            var tieredName = rarity + cleanName;
            
            String uuid = extractUUID(stack);
            var dropResult = ItemTrackerData.addDrop(tieredName, 1, null, uuid);
            
            if (dropResult.shouldNotify) {
                FishyNotis.trackerNoti(displayName, 1);
            }
        }
    }

    private static String getRarityTier(Style style) {
        String def = "common ";

        if (style == null || style.getColor() == null) {
            return def;
        }

        switch (style.getColor().toString()) {
            case "white": return def;
            case "blue": return "rare ";
            case "dark_purple": return "epic ";
            case "gold": return "legendary ";
            case "light_purple": return "mythic ";
            default: return def;
        }
    }

    public static void handleBookAdded(ItemStack stack) {

        var lore = stack.get(DataComponentTypes.LORE);
        if (lore == null) return;

        var bookInfo = extractBookInfoFromLore(lore);
        if (bookInfo == null) return;
        
        String uuid = extractUUID(stack);
        var dropResult = ItemTrackerData.addDrop(bookInfo.name, 1, null, uuid);
        
        if (!dropResult.alreadyCounted && dropResult.shouldNotify) {
            FishyNotis.bookNoti(bookInfo.styledText);
        }
    }

    private static String extractUUID(ItemStack stack) {
        return ItemData.extractUUID(stack);
    }

    private static class BookInfo {
        final String name;
        final Text styledText;
        
        BookInfo(String name, Text styledText) {
            this.name = name;
            this.styledText = styledText;
        }
    }
    
    private static BookInfo extractBookInfoFromLore(LoreComponent lore) {
        for (Text line : lore.lines()) {
            var firstText = FromText.firstLiteral(line);
            
            if (firstText != null) {
                var plainName = firstText.getString();
                var numericName = toNumeric(plainName);
                var ultimateText = FromText.findNodeWithColor(line, Formatting.LIGHT_PURPLE);

                if (ultimateText != null) {
                    var styledUltimate = Text.literal(plainName)
                        .styled(style -> style.withColor(Formatting.LIGHT_PURPLE).withBold(true));
                    return new BookInfo(ULTIMATE_PREFIX + numericName, styledUltimate);

                } else {
                    return new BookInfo(numericName, firstText.copy());
                }
            }
        }

        return null;
    }

    private static String toNumeric(String plainName) {
        String[] parts = plainName.split(" ");
        if (parts.length < 2) {
            return plainName;
        }

        String tier = parts[parts.length - 1];
        String baseName = String.join(" ", Arrays.copyOf(parts, parts.length - 1));

        return baseName + " " + romanToNumeric(tier);
    }

    private static String romanToNumeric(String roman) {
        switch (roman.toLowerCase()) {
            case "i": return "1";
            case "ii": return "2";
            case "iii": return "3";
            case "iv": return "4";
            case "v": return "5";
            case "vi": return "6";
            case "vii": return "7";
            case "viii": return "8";
            case "ix": return "9";
            case "x": return "10";
            default: return roman;
        }
    }

    /**
     * Cleanup method to be called periodically to reset correlation state
     */
    public static void cleanup() {
        long currentTime = System.currentTimeMillis();

        recentTrackedItemDrops.entrySet().removeIf(entry -> 
            (currentTime - entry.getKey()) > DROP_CORRELATION_WINDOW * 3);

        if (lastKnownStackSizes.size() > 63) {
            lastKnownStackSizes.clear();
        }
    }
    
    /**
     * Called when a loot share message is detected
     */
    public static void onLsDetected() {
        lsEnabled = true;
        lsStartTime = System.currentTimeMillis();
    }
    
    /**
     * Called when a relevant entity is found nearby
     */
    public static void onValuableFound() {
        if (!monitoringEnabled) {
            monitoringEnabled = true;
        }
        monitoringStartTime = System.currentTimeMillis();
    }
    
    public static void onValuableGone() {

        monitoringEnabled = true;
        monitoringStartTime = System.currentTimeMillis();
        
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(500); 
                if (System.currentTimeMillis() - monitoringStartTime >= 200 && monitoringEnabled) {
                    monitoringEnabled = false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
