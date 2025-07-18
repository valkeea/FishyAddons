package me.valkeea.fishyaddons.hud;

import me.valkeea.fishyaddons.tracker.ItemTrackerData;
import java.util.HashMap;
import java.util.Map;

public class HudDisplayCache {
    private static HudDisplayCache instance;
    private CachedHudData cachedData;
    private long lastApiUpdate = 0;
    private static final long CACHE_VALIDITY_MS = 5000;
    private static final long API_STALENESS_THRESHOLD = 3000000;
    private HudDisplayCache() {}

    public static HudDisplayCache getInstance() {
        if (instance == null) {
            instance = new HudDisplayCache();
        }
        return instance;
    }
    
    public CachedHudData getDisplayData() {
        long currentTime = System.currentTimeMillis();
        long apiTimestamp = Math.max(
            ItemTrackerData.getLastBazaarUpdateTime(),
            ItemTrackerData.getLastAuctionUpdateTime()
        );
        
        boolean needsRefresh = cachedData == null ||
                             apiTimestamp > lastApiUpdate ||
                             (cachedData.cacheTime > 0 && (currentTime - cachedData.cacheTime) > CACHE_VALIDITY_MS);
        
        if (needsRefresh) {
            refreshCache(apiTimestamp, currentTime);
        }
        
        return cachedData;
    }
    
    public void invalidateCache() {
        cachedData = null;
        lastApiUpdate = 0;
    }
    
    private void refreshCache(long apiTimestamp, long currentTime) {
        Map<String, Integer> items = ItemTrackerData.getAllItems();
        
        // Get item values from ItemTrackerData cache
        Map<String, Double> itemValues = new HashMap<>();
        for (String itemName : items.keySet()) {
            double unitPrice = ItemTrackerData.getCachedItemValue(itemName);
            if (unitPrice > 0) {
                itemValues.put(itemName, unitPrice);
            }
        }
        
        // Pre-calculate commonly used values
        int totalItems = ItemTrackerData.getTotalItemCount();
        double totalValue = ItemTrackerData.getTotalSessionValue();
        long sessionDuration = ItemTrackerData.getSessionDurationMinutes();
        
        boolean hasRecentApiData = apiTimestamp > 0 && (currentTime - apiTimestamp) < API_STALENESS_THRESHOLD;
        String formattedValue = formatCoins(totalValue);
        String timeString = sessionDuration > 0 ? String.format(" (%dm)", sessionDuration) : "";
        String apiIndicator = calculateApiIndicator(hasRecentApiData);
        
        cachedData = new CachedHudDataBuilder()
            .setItems(items)
            .setItemValues(itemValues)
            .setTotalItems(totalItems)
            .setTotalValue(totalValue)
            .setFormattedValue(formattedValue)
            .setTimeString(timeString)
            .setApiIndicator(apiIndicator)
            .setHasRecentApiData(hasRecentApiData)
            .setCacheTime(currentTime)
            .build();
        
        lastApiUpdate = apiTimestamp;
    }
    
    // Calculate the API indicator for bazaar, ah is on-demand
    private String calculateApiIndicator(boolean hasRecentData) {
        long lastBazaarUpdate = ItemTrackerData.getLastBazaarUpdateTime();
        if (lastBazaarUpdate > 0) {
            return hasRecentData ? " §a●" : " §c●";
        } else {
            return " §7●";
        }
    }
    
    private String formatCoins(double coins) {
        if (coins == 0) return "0 coins";
        
        if (coins >= 1_000_000_000.0) {
            return String.format("%.1fb coins", coins / 1_000_000_000.0);
        } else if (coins >= 1_000_000.0) {
            return String.format("%.1fm coins", coins / 1_000_000.0);
        } else if (coins >= 1_000.0) {
            return String.format("%.1fk coins", coins / 1_000.0);
        } else {
            return String.format("%.0f coins", coins);
        }
    }
    
    public static class CachedHudData {
        public final Map<String, Integer> items;
        public final Map<String, Double> itemValues; // Unit price per item
        public final int totalItems;
        public final double totalValue;
        public final String formattedValue;
        public final String timeString;
        public final String apiIndicator;
        public final boolean hasRecentApiData;
        public final long cacheTime;
        
        CachedHudData(CachedHudDataBuilder builder) {
            this.items = builder.items;
            this.itemValues = builder.itemValues;
            this.totalItems = builder.totalItems;
            this.totalValue = builder.totalValue;
            this.formattedValue = builder.formattedValue;
            this.timeString = builder.timeString;
            this.apiIndicator = builder.apiIndicator;
            this.hasRecentApiData = builder.hasRecentApiData;
            this.cacheTime = builder.cacheTime;
        }
        
        public boolean isEmpty() {
            return items.isEmpty();
        }
    }
    
    
    // Builder for CachedHudData to avoid too many constructor parameters
    private static class CachedHudDataBuilder {
        Map<String, Integer> items;
        Map<String, Double> itemValues;
        int totalItems;
        double totalValue;
        String formattedValue;
        String timeString;
        String apiIndicator;
        boolean hasRecentApiData;
        long cacheTime;
        
        CachedHudDataBuilder setItems(Map<String, Integer> items) {
            this.items = items;
            return this;
        }
        
        CachedHudDataBuilder setItemValues(Map<String, Double> itemValues) {
            this.itemValues = itemValues;
            return this;
        }
        
        CachedHudDataBuilder setTotalItems(int totalItems) {
            this.totalItems = totalItems;
            return this;
        }
        
        CachedHudDataBuilder setTotalValue(double totalValue) {
            this.totalValue = totalValue;
            return this;
        }
        
        CachedHudDataBuilder setFormattedValue(String formattedValue) {
            this.formattedValue = formattedValue;
            return this;
        }
        
        CachedHudDataBuilder setTimeString(String timeString) {
            this.timeString = timeString;
            return this;
        }
        
        CachedHudDataBuilder setApiIndicator(String apiIndicator) {
            this.apiIndicator = apiIndicator;
            return this;
        }
        
        CachedHudDataBuilder setHasRecentApiData(boolean hasRecentApiData) {
            this.hasRecentApiData = hasRecentApiData;
            return this;
        }
        
        CachedHudDataBuilder setCacheTime(long cacheTime) {
            this.cacheTime = cacheTime;
            return this;
        }
        
        CachedHudData build() {
            return new CachedHudData(this);
        }
    }
}
