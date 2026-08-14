package me.valkeea.fishyaddons.tracker.profit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.valkeea.fishyaddons.tool.ItemData;
import me.valkeea.fishyaddons.tool.RunDelayed;
import me.valkeea.fishyaddons.tracker.PriceUtil;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.FromText;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class InventoryTracker {

    private static final int STACK_INCREASE_THRESHOLD = 32;
    private static final long DROP_CORRELATION_WINDOW = 2000;
    private static final long MONITORING_WINDOW = 180000;
    private static final long LS_WINDOW = 1000;

    private static final String CLEAN_REGEX = "§[0-9a-fk-or]";

    private static final List<String> TRACKED_PLAYER_HEADS = new ArrayList<>();
    private static final List<String> TRACKED_GHAST_TEARS = new ArrayList<>();
    private static final List<String> TRACKED_TOOLS = new ArrayList<>();    

    private static boolean monitoringEnabled = false;
    private static boolean lsEnabled = false;

    private static long monitoringStartTime = 0;
    private static long lsStartTime = 0;

    private static final Map<String, String> rarityColorMap = Map.of(
        "common", "white",
        "uncommon", "green",
        "rare", "blue",
        "epic", "dark_purple",
        "legendary", "gold",
        "mythic", "light_purple",
        "divine", "aqua",
        "special", "red",
        "ultimate", "dark_red"
    );
    
    static {
        TRACKED_PLAYER_HEADS.add("emperor's skull");
        TRACKED_PLAYER_HEADS.add("magma lord fragment");
        TRACKED_PLAYER_HEADS.add("soul fragment");
        TRACKED_PLAYER_HEADS.add("isopod husk");        
        TRACKED_PLAYER_HEADS.add("foraging exp boost");
        TRACKED_PLAYER_HEADS.add("minos relic");
        TRACKED_PLAYER_HEADS.add("dwarf turtle shelmet");
        TRACKED_PLAYER_HEADS.add("water hydra head");
        TRACKED_GHAST_TEARS.add("great white shark tooth");
        TRACKED_TOOLS.add("fishing exp boost");
        TRACKED_TOOLS.add("foraging exp boost");
        TRACKED_TOOLS.add("combat exp boost");         
    }
    
    private static final Map<Long, String> recentTrackedItemDrops = new ConcurrentHashMap<>();
    private static final Map<String, Integer> lastKnownStackSizes = new ConcurrentHashMap<>();

    private InventoryTracker() {}
    
    public static void onItemAdded(ItemStack stack) {
        if (stack.isEmpty() || !isMonitoringActive()) return;

        var newItem = stack.getItem();
        if (newItem == Items.PLAYER_HEAD || newItem == Items.PAPER) {
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
        var displayName = stack.getHoverName();
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
            var dropResult = TrackedItemData.addDrop(cleanName, newItems, uuid);
            
            if (dropResult.shouldNotify) {
                FishyNotis.trackerNoti(displayName, newItems);
            }

        } else {
            lastKnownStackSizes.put(cleanName, currentStackSize);
        }
    }

    private static void handleGhastTearAdded(ItemStack stack) {
        var displayName = stack.getHoverName();
        var cleanName = displayName.getString().toLowerCase().replaceAll(CLEAN_REGEX, "").trim();

        if (TRACKED_GHAST_TEARS.contains(cleanName)) {
            String uuid = extractUUID(stack);
            var dropResult = TrackedItemData.addDrop(cleanName, 1, uuid);
            
            if (dropResult.shouldNotify) {
                FishyNotis.trackerNoti(displayName, 1);
            }
        }
    }

    private static void handleToolAdded(ItemStack stack) {
        var displayName = stack.getHoverName();
        var cleanName = displayName.getString().toLowerCase().replaceAll(CLEAN_REGEX, "").trim();      

        if (TRACKED_TOOLS.contains(cleanName)) {
            var rarity = getRarityTier(displayName);
            var tieredName = rarity + cleanName;
            
            String uuid = extractUUID(stack);
            var dropResult = TrackedItemData.addDrop(tieredName, 1, uuid);
            
            if (dropResult.shouldNotify) {
                FishyNotis.trackerNoti(displayName, 1);
            }
        }
    }

    public static String getRarityTier(Component text) {

        var color = FromText.findFirstTextColor(text, InventoryTracker::isRarityColor);
        var def = "common ";
        if (color == null) return def;

        return rarityColorMap.entrySet().stream()
            .filter(entry -> entry.getValue().equals(color.toString()))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(def) + " ";
    }

    public static boolean isRarityColor(TextColor color) {
        return rarityColorMap.containsValue(color.toString());
    }

    public static void handleBookAdded(ItemStack stack) {

        var lore = stack.get(DataComponents.LORE);
        if (lore == null) return;

        var bookInfo = PriceUtil.getBookInfo(lore);
        if (bookInfo == null) return;
        
        String uuid = extractUUID(stack);
        var dropResult = TrackedItemData.addDrop(bookInfo.name(), 1, uuid);

        if (!dropResult.alreadyCounted && dropResult.shouldNotify) {
            FishyNotis.bookNoti(bookInfo.styledName());
        }
    }

    private static String extractUUID(ItemStack stack) {
        return ItemData.extractUUID(stack);
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

    private static void disableIfActive() {
        if (monitoringEnabled) {
            monitoringEnabled = false;
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
        
        RunDelayed.run(
            InventoryTracker::disableIfActive,
            DROP_CORRELATION_WINDOW,
            "valuableGone_" + monitoringStartTime
        );
    }
}
