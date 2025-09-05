package me.valkeea.fishyaddons.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.cache.ApiCache;
import me.valkeea.fishyaddons.config.Key;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

/**
 * Parses Hypixel Skyblock sack notification messages with hover events to extract item drops
 */
public class SackDropParser {
    private SackDropParser() {}
    
    private static final String SACK_HOVER_PREFIX = "SACK_HOVER:";
    private static final String SACK_CACHE_PREFIX = "SACK:";
    private static boolean shouldTrackSack = false;
    
    // Deduplication mechanism to prevent processing same tooltip multiple times
    private static final java.util.Map<String, Long> recentlyProcessedTooltips = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long DEDUP_WINDOW_MS = 1000; // 1 second window

    // Chat drop deduplication mechanism with quantity tracking
    private static final java.util.Map<String, ChatDropEntry> recentChatDrops = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Inner class to store chat drop information
    private static class ChatDropEntry {
        final int quantity;
        final long timestamp;
        
        ChatDropEntry(int quantity, long timestamp) {
            this.quantity = quantity;
            this.timestamp = timestamp;
        }
    }
    
    public static boolean isOn() {
        return shouldTrackSack;
    }

    public static void refresh() {
        shouldTrackSack = me.valkeea.fishyaddons.config.FishyConfig.getState(Key.TRACK_SACK, false) && 
            me.valkeea.fishyaddons.config.FishyConfig.getState(Key.HUD_TRACKER_ENABLED, false);
    }

    public static void toggle() {
        me.valkeea.fishyaddons.config.FishyConfig.toggle(Key.TRACK_SACK, false);
        refresh();
    }
    
    // Pattern to match sack notification messages
    private static final Pattern SACK_MESSAGE_PATTERN = Pattern.compile(
        "\\[Sacks\\]\\s*\\+\\d+\\s*items?\\.\\s*\\(Last\\s+\\d+[smh]\\.\\)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern to extract item drops from hover text
    private static final Pattern HOVER_DROP_PATTERN = Pattern.compile(
        "\\+\\s*(\\d+)\\s+(.+?)(?:\\s*\\([^)]*\\ssack\\))?$",
        Pattern.CASE_INSENSITIVE
    );
    
    public static boolean isSackNotification(String message) {
        if (!shouldTrackSack) {
            return false;
        }
        
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        String cleanMessage = message.replaceAll("§[0-9a-fk-or]", "").trim();
        return SACK_MESSAGE_PATTERN.matcher(cleanMessage).find();
    }
    
    public static List<ChatDropParser.ParseResult> parseSackHoverEvent(HoverEvent hoverEvent) {
        List<ChatDropParser.ParseResult> results = new ArrayList<>();
        if (!shouldTrackSack) {
            return results;
        }
        
        if (hoverEvent == null || hoverEvent.getAction() != HoverEvent.Action.SHOW_TEXT) {
            return results;
        }
        
        String tooltipContent = extractTooltipContent(hoverEvent);
        if (tooltipContent == null || tooltipContent.trim().isEmpty()) {
            return results;
        }
        
        // Deduplication check
        String tooltipHash = String.valueOf(tooltipContent.hashCode());
        long currentTime = System.currentTimeMillis();
        Long lastProcessed = recentlyProcessedTooltips.get(tooltipHash);
        if (lastProcessed != null && (currentTime - lastProcessed) < DEDUP_WINDOW_MS) {
            return results;
        }
        
        recentlyProcessedTooltips.put(tooltipHash, currentTime);
        cleanupOldTooltipEntries(currentTime);
        Object cachedResult = ApiCache.getCachedMessageParse(SACK_HOVER_PREFIX + tooltipContent);
        if (cachedResult != null) {
            if (ApiCache.isNullParseResult(cachedResult)) {
                return results; // Cache hit: no items found
            }
            @SuppressWarnings("unchecked")
            List<ChatDropParser.ParseResult> cachedResults = (List<ChatDropParser.ParseResult>) cachedResult;
            return cachedResults;
        }
        
        results = parseTooltipContent(tooltipContent);
        if (!results.isEmpty()) {
            clearChatDrops();
        }
        
        // Cache the results
        if (results.isEmpty()) {
            ApiCache.cacheMessageParse(SACK_HOVER_PREFIX + tooltipContent, null);
        } else {
            ApiCache.cacheMessageParse(SACK_HOVER_PREFIX + tooltipContent, results);
        }

        recentlyProcessedTooltips.put(tooltipContent, currentTime);
        
        return results;
    }
    
    /**
     * Clean up old tooltip entries
     */
    private static void cleanupOldTooltipEntries(long currentTime) {
        recentlyProcessedTooltips.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > DEDUP_WINDOW_MS * 2);
    }
    
    /**
     * Extract tooltip content from hover event
     */
    private static String extractTooltipContent(HoverEvent hoverEvent) {
        try {
            if (hoverEvent instanceof HoverEvent.ShowText showText) {
                Text tooltipText = showText.value();
                return tooltipText.getString();
            }
        } catch (Exception e) {
            // Continue to fallback
        }
        
        try {
            java.lang.reflect.Method getValueMethod = hoverEvent.getClass().getMethod("getValue", HoverEvent.Action.class);
            Object value = getValueMethod.invoke(hoverEvent, hoverEvent.getAction());
            
            if (value instanceof Text tooltipText) {
                return tooltipText.getString();
            }
        } catch (Exception e) {
            // Failed to extract tooltip - return null
        }
        
        return null;
    }
    
    /**
     * Parse tooltip content to extract individual item drops
     */
    private static List<ChatDropParser.ParseResult> parseTooltipContent(String tooltipContent) {
        List<ChatDropParser.ParseResult> results = new ArrayList<>();
        String[] lines = tooltipContent.split("\\n");
        for (String line : lines) {
            String cleanLine = line.trim();
            if (cleanLine.isEmpty()) {
                continue;
            }
            
            ChatDropParser.ParseResult result = parseTooltipLine(cleanLine);
            if (result != null) {
                results.add(result);
            }
        }
        
        return results;
    }
    
    /**
     * Parse a single tooltip line for item drops, account for already registered chat drops
     */
    private static ChatDropParser.ParseResult parseTooltipLine(String line) {
        Matcher matcher = HOVER_DROP_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        
        try {
            int sackQuantity = Integer.parseInt(matcher.group(1));
            String itemName = matcher.group(2).trim();

            itemName = cleanSackItemName(itemName);

            if (isTrackableSackItem(itemName)) {
                int chatQuantity = getChatDropQuantity(itemName);
                if (chatQuantity > 0) {
                    int remainingQuantity = sackQuantity - chatQuantity;
                    
                    if (remainingQuantity <= 0) {
                        return null;
                    } else {
                        return new ChatDropParser.ParseResult(itemName, remainingQuantity, false);
                    }
                }

                return new ChatDropParser.ParseResult(itemName, sackQuantity, false);
            }
        } catch (NumberFormatException e) {
            // Skip invalid quantity
        }
        
        return null;
    }
    
    /**
     * Clean sack item names
     */
    private static String cleanSackItemName(String itemName) {
        if (itemName == null) {
            return "";
        }
    
        String cachedResult = ApiCache.getCachedNormalization(SACK_CACHE_PREFIX + itemName);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        String cleaned = itemName.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\"'`]", "")
                .replace("✯", "")
                .trim()
                .toLowerCase();
        
        ApiCache.cacheNormalization(SACK_CACHE_PREFIX + itemName, cleaned);
    
        return cleaned;
    }
    
    /**
     * Check if a sack item should be tracked
     */
    private static boolean isTrackableSackItem(String itemName) {
        return itemName != null && !itemName.trim().isEmpty();
    }
    
    /**
     * Get the quantity of a recent chat drop for an item
     * @param itemName The normalized item name
     * @return The quantity from the recent chat drop, or 0 if no recent drop
     */
    private static int getChatDropQuantity(String itemName) {
        // Check if this item was recently processed via chat
        ChatDropEntry entry = recentChatDrops.get(itemName);
        if (entry != null) {
            return entry.quantity;
        }
        return 0;
    }
    
    private static void clearChatDrops() {
        recentChatDrops.clear();
    }
    
    /**
     * Register a chat drop to prevent sack duplicates
     * @param itemName The item name from chat parsing
     * @param quantity The quantity of the drop
     */
    public static void registerChatDrop(String itemName, int quantity) {
        if (!shouldTrackSack) {
            return; // No need to track if sack parsing is disabled
        }

        String normalizedItemName = cleanSackItemName(itemName);
        recentChatDrops.put(normalizedItemName, new ChatDropEntry(quantity, System.currentTimeMillis()));
    }
}