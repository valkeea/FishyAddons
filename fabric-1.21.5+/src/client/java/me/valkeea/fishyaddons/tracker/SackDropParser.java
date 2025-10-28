package me.valkeea.fishyaddons.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.cache.ApiCache;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;

public class SackDropParser {
    private SackDropParser() {}
    
    private static final String SACK_HOVER_PREFIX = "SACK_HOVER:";
    private static final String SACK_CACHE_PREFIX = "SACK:";
    private static boolean shouldTrackSack = false;
    
    private static final java.util.Map<String, Long> recentlyProcessedTooltips = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, Integer> recentChatDrops = new java.util.concurrent.ConcurrentHashMap<>();   

    private static final long DEDUP_WINDOW_MS = 1000;
    
    public static boolean isOn() {
        return shouldTrackSack;
    }

    public static void refresh() {
        shouldTrackSack = FishyConfig.getState(Key.TRACK_SACK, false) && 
            FishyConfig.getState(Key.HUD_TRACKER_ENABLED, false);
    }

    public static void toggle() {
        FishyConfig.toggle(Key.TRACK_SACK, false);
        refresh();
    }
    
    private static final Pattern HOVER_DROP_PATTERN = Pattern.compile(
        "\\+\\s*(\\d{1,3}(?:,\\d{3})*)\\s+(.+?)(?:\\s*\\([^)]*\\ssack\\))?$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BAZAAR_PATTERN = Pattern.compile(
        "\\[Bazaar\\]\\s*Bought\\s*(\\d{1,3}(?:,\\d{3})*)x\\s*(.+?)\\s*for",
        Pattern.CASE_INSENSITIVE
    );
    
    public static List<ChatDropParser.ParseResult> parseSackHoverEvent(String tooltipContent) {

        List<ChatDropParser.ParseResult> results = new ArrayList<>();

        if (tooltipContent == null || tooltipContent.trim().isEmpty()) {
            return results;
        }

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
                return results;
            }
            @SuppressWarnings("unchecked")
            List<ChatDropParser.ParseResult> cachedResults = (List<ChatDropParser.ParseResult>) cachedResult;
            return cachedResults;
        }

        results = parseTooltipContent(tooltipContent);
        if (!results.isEmpty()) {
            clearChatDrops();
        }

        if (results.isEmpty()) {
            ApiCache.cacheMessageParse(SACK_HOVER_PREFIX + tooltipContent, null);
        } else {
            ApiCache.cacheMessageParse(SACK_HOVER_PREFIX + tooltipContent, results);
        }

        recentlyProcessedTooltips.put(tooltipContent, currentTime);
        return results;
    }
    
    private static void cleanupOldTooltipEntries(long currentTime) {
        recentlyProcessedTooltips.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > DEDUP_WINDOW_MS * 2);
    }
    
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
    
    private static ChatDropParser.ParseResult parseTooltipLine(String line) {
        var matcher = HOVER_DROP_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        
        try {
            String quantityStr = matcher.group(1).replace(",", "");
            int sackQuantity = Integer.parseInt(quantityStr);
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
    
    private static boolean isTrackableSackItem(String itemName) {
        return itemName != null && !itemName.trim().isEmpty();
    }
    
    /** Get the quantity of a recent chat drop for an item */
    private static int getChatDropQuantity(String itemName) {
        return recentChatDrops.get(itemName) == null ? 0 : recentChatDrops.get(itemName);
    }
    
    private static void clearChatDrops() {
        recentChatDrops.clear();
    }
    
    /** Register a chat drop to prevent sack duplicates */
    public static void registerChatDrop(String itemName, int quantity) {
        if (!shouldTrackSack) {
            return;
        }

        String normalizedItemName = cleanSackItemName(itemName);
        recentChatDrops.put(normalizedItemName, quantity);
    }

    /** Register bazaar purchases as chat drops */
    public static void onBazaarBuy(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        Matcher matcher = BAZAAR_PATTERN.matcher(message);
        if (matcher.find()) {

            try {
                String quantityStr = matcher.group(1).replace(",", "");
                int quantity = Integer.parseInt(quantityStr);
                String itemName = matcher.group(2).trim();
                
                itemName = cleanSackItemName(itemName);
                
                if (isTrackableSackItem(itemName) && quantity > 0) {
                    registerChatDrop(itemName, quantity);
                }

            } catch (NumberFormatException e) {
                // Ignore invalid number format
            }
        }
    }
}