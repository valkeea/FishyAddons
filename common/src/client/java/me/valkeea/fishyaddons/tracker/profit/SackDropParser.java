package me.valkeea.fishyaddons.tracker.profit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.cache.ApiCache;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.tracker.profit.ChatDropParser.ParseResult;

public class SackDropParser {
    private SackDropParser() {}
    
    private static final String SACK_HOVER_PREFIX = "SACK_HOVER:";
    private static final String SACK_CACHE_PREFIX = "SACK:";
    private static boolean shouldTrackSack = false;
    
    private static final Map<String, Long> seenTooltips = new ConcurrentHashMap<>();
    private static final Map<String, Integer> chatDrops = new ConcurrentHashMap<>();   

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

    private static final Pattern BAZAAR_SELL_CANCEL_PATTERN = Pattern.compile(
        "\\[Bazaar\\]\\s*Cancelled! Refunded\\s*(\\d{1,3}(?:,\\d{3})*)x\\s*(.+?)\\s*from",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BAZAAR_CLAIM_PATTERN = Pattern.compile(
        "\\[Bazaar\\]\\s*Claimed\\s*(\\d{1,3}(?:,\\d{3})*)x\\s*(.+?)\\s*worth",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SUPERCRAFT_PATTERN = Pattern.compile(
        "You Supercrafted\\s+(.+?)\\s*x?(\\d{1,3}(?:,\\d{3})*)?\\s*!",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SACK_TO_INVENTORY_PATTERN = Pattern.compile(
        "Moved\\s*(\\d{1,3}(?:,\\d{3})*)\\s+(.+?)\\s*from your Sacks to your inventory",
         Pattern.CASE_INSENSITIVE
    );    
    
    public static List<ParseResult> parseSackHoverEvent(String tooltipContent) {

        List<ParseResult> results = new ArrayList<>();

        if (tooltipContent == null || tooltipContent.trim().isEmpty()) {
            return results;
        }

        String tooltipHash = String.valueOf(tooltipContent.hashCode());
        long currentTime = System.currentTimeMillis();
        Long lastProcessed = seenTooltips.get(tooltipHash);
        if (lastProcessed != null && (currentTime - lastProcessed) < DEDUP_WINDOW_MS) {
            return results;
        }

        seenTooltips.put(tooltipHash, currentTime);
        cleanup(currentTime);
        Object cachedResult = ApiCache.getCachedMessageParse(SACK_HOVER_PREFIX + tooltipContent);

        if (cachedResult != null) {
            if (ApiCache.isNullParseResult(cachedResult)) {
                return results;
            }
            @SuppressWarnings("unchecked")
            List<ParseResult> cachedResults = (List<ParseResult>) cachedResult;
            return cachedResults;
        }

        results = parse(tooltipContent);
        if (!results.isEmpty()) {
            clearChatDrops();
        }

        if (results.isEmpty()) {
            ApiCache.cacheMessageParse(SACK_HOVER_PREFIX + tooltipContent, null);
        } else {
            ApiCache.cacheMessageParse(SACK_HOVER_PREFIX + tooltipContent, results);
        }

        seenTooltips.put(tooltipContent, currentTime);
        return results;
    }
    
    private static void cleanup(long currentTime) {
        seenTooltips.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > DEDUP_WINDOW_MS * 2);
    }
    
    private static List<ParseResult> parse(String tooltipContent) {
        List<ParseResult> results = new ArrayList<>();
        String[] lines = tooltipContent.split("\\n");
        for (String line : lines) {
            String cleanLine = line.trim();
            if (cleanLine.isEmpty()) {
                continue;
            }
            
            ParseResult result = parseLine(cleanLine);
            if (result != null) {
                results.add(result);
            }
        }
        
        return results;
    }
    
    private static ParseResult parseLine(String line) {
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
                        return new ParseResult(itemName, remainingQuantity, false);
                    }
                }

                return new ParseResult(itemName, sackQuantity, false);
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

    // --- Chat processor ---

    public static boolean notGain(String s) {
        if (s.startsWith("[bazaar]")) return onBazaar(s);
        if (s.startsWith("you supercrafted")) return onSupercraft(s);
        if (s.startsWith("moved")) return onGfsMove(s);
        return false;
    }    

    public static boolean onBazaar(String message) {
        if (message == null || message.trim().isEmpty()) return false;

        Matcher m = BAZAAR_PATTERN.matcher(message);
        if (m.find() && tryParse(m.group(2), m.group(1))) {
            return true;
        }

        Matcher cm = BAZAAR_CLAIM_PATTERN.matcher(message);
        if (cm.find() && tryParse(cm.group(2), cm.group(1))) {
            return true;
        }

        Matcher scm = BAZAAR_SELL_CANCEL_PATTERN.matcher(message);
        return (scm.find() && tryParse(scm.group(2), scm.group(1)));
    }

    public static boolean onSupercraft(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        Matcher m = SUPERCRAFT_PATTERN.matcher(message);
        return (m.find() && tryParse(m.group(1), m.group(2) != null ? m.group(2) : "1"));
    }

    public static boolean onGfsMove(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        Matcher m = SACK_TO_INVENTORY_PATTERN.matcher(message);
        return m.find() && tryParse(m.group(2), m.group(1));
    }

    private static boolean tryParse(String itemName, String quantityStr) {
        if (itemName == null || itemName.trim().isEmpty() || quantityStr == null || quantityStr.trim().isEmpty()) {
            return false;
        }

        try {
            int quantity = Integer.parseInt(quantityStr.replace(",", "").trim());
            registerIfValid(itemName, quantity);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void registerIfValid(String itemName, int quantity) {
        itemName = cleanSackItemName(itemName.trim());
        if (isTrackableSackItem(itemName) && quantity > 0) {
            registerChatDrop(itemName, quantity);
        }
    }

    /** Register a chat drop to prevent sack duplicates */
    public static void registerChatDrop(String itemName, int quantity) {
        if (!shouldTrackSack) {
            return;
        }

        String normalizedItemName = cleanSackItemName(itemName.trim());
        if (chatDrops.containsKey(normalizedItemName)) {
            int existingQuantity = chatDrops.get(normalizedItemName);
            chatDrops.put(normalizedItemName, existingQuantity + quantity);
        } else {
            chatDrops.put(normalizedItemName, quantity);
        }
    }
    
    /** Get the quantity of a recent chat drop for an item */
    private static int getChatDropQuantity(String itemName) {
        return chatDrops.get(itemName) == null ? 0 : chatDrops.get(itemName);
    }
    
    private static void clearChatDrops() {
        chatDrops.clear();
    }    
}
