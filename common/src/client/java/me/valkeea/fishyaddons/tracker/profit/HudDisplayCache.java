package me.valkeea.fishyaddons.tracker.profit;

import java.util.Map;

import me.valkeea.fishyaddons.api.hypixel.PriceServiceManager;
import me.valkeea.fishyaddons.hud.core.HudUtils;
import me.valkeea.fishyaddons.tracker.PriceUtil;

@SuppressWarnings("squid:S6548")
public class HudDisplayCache {
    private static HudDisplayCache instance;
    private CachedHudData cachedData;
    private long lastApiUpdate = 0;
    private static final long CACHE_VALIDITY_MS = 10000;
    private static final long API_STALENESS_THRESHOLD = 2L * 60 * 60 * 1000;
    private HudDisplayCache() {}

    public static HudDisplayCache getInstance() {
        if (instance == null) {
            instance = new HudDisplayCache();
        }
        return instance;
    }

    public boolean hasData() {
        return cachedData != null;
    }

    public CachedHudData getDisplayData() {
        long currentTime = System.currentTimeMillis();
        long apiTimestamp = PriceUtil.getLastApiUpdateTime();
        
        boolean needsRefresh = cachedData == null || apiTimestamp > lastApiUpdate ||
                            !PriceServiceManager.isInitialized() ||
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

        var items = TrackedItemData.getAllItems();
        var itemValues = TrackedItemData.getPrices();
        int totalItems = TrackedItemData.getTotalItemCount();
        double totalValue = TrackedItemData.getTotalSessionValue();
        long sessionDuration = TrackedItemData.getTotalDurationMinutes();
        
        boolean hasRecentApiData = apiTimestamp > 0 && (currentTime - apiTimestamp) < API_STALENESS_THRESHOLD;
        String formattedValue = HudUtils.formatCoins(totalValue);
        String timeString = sessionDuration > 60 ? String.format(" (%dh %dmin)", sessionDuration / 60, sessionDuration % 60) : String.format(" (%dmin)", sessionDuration);
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

    public int getSize() {
        return cachedData != null ? cachedData.items.size() : 0;
    }
    
    private String calculateApiIndicator(boolean hasRecentData) {
        if (PriceUtil.getLastApiUpdateTime() > CACHE_VALIDITY_MS) {
            return hasRecentData ? " §a●" : " §7●";
        } else {
            return " §7●";
        }
    }

    public static class CachedHudData {
        public final Map<String, Integer> items;
        public final Map<String, Double> itemValues;
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
