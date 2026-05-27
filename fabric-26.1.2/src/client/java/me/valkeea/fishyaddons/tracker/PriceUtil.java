package me.valkeea.fishyaddons.tracker;

import me.valkeea.fishyaddons.api.hypixel.PriceServiceManager;
import me.valkeea.fishyaddons.tracker.collection.CollectionTracker;
import me.valkeea.fishyaddons.tracker.profit.ProfitTracker;
import me.valkeea.fishyaddons.tracker.profit.SackDropParser;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;

public class PriceUtil {
    private PriceUtil() {}

    public static void refresh() {
        
        boolean profit = Config.get(BooleanKey.HUD_PROFIT_ENABLED);
        boolean pricePerItem = Config.get(BooleanKey.PER_ITEM);
        boolean collection = Config.get(BooleanKey.HUD_COLLECTION_ENABLED);
        boolean sack = (Config.get(BooleanKey.TRACK_SACK) && profit) || collection; 

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
