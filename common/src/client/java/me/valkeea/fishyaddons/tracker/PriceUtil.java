package me.valkeea.fishyaddons.tracker;

import me.valkeea.fishyaddons.api.hypixel.PriceServiceManager;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.tracker.collection.CollectionTracker;
import me.valkeea.fishyaddons.tracker.profit.ProfitTracker;
import me.valkeea.fishyaddons.tracker.profit.SackDropParser;

public class PriceUtil {
    private PriceUtil() {}

    public static void refresh() {
        
        boolean profit = FishyConfig.getState(Key.HUD_PROFIT_ENABLED, false);
        boolean pricePerItem = FishyConfig.getState(Key.PER_ITEM, false);
        boolean collection = FishyConfig.getState(Key.HUD_COLLECTION_ENABLED, false);
        boolean sack = (FishyConfig.getState(Key.TRACK_SACK, false) && profit) || collection; 

        if (!PriceServiceManager.isInitialized() && (profit || collection))  {
            PriceServiceManager.initialize();
        }

        CollectionTracker.initIfNeeded(collection); 
        SackDropParser.setTracking(sack);
        ProfitTracker.setConfig(profit, sack, pricePerItem);
    }

    public static void shutdown() {
        PriceServiceManager.shutdown();
    } 

    public static long getLastApiUpdateTime() {
        var service = PriceServiceManager.getInstanceOrNull();
        return service != null ? service.getLastBazaarUpdate() : 0;
    }
    
    /**
     * Attempt to refresh all prices (bazaar and auctions)
     */
    public static void refreshPrices() {
        var service = PriceServiceManager.getInstanceOrNull();
        if (service != null ) service.refreshAllAsync();
    }  
}
