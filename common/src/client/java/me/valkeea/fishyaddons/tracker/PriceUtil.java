package me.valkeea.fishyaddons.tracker;

import java.text.DecimalFormat;

import me.valkeea.fishyaddons.api.hypixel.PriceServiceManager;
import me.valkeea.fishyaddons.tracker.collection.CollectionTracker;
import me.valkeea.fishyaddons.tracker.profit.ProfitTracker;
import me.valkeea.fishyaddons.tracker.profit.SackDropParser;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;

public class PriceUtil {
    private PriceUtil() {}

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,###.##");

    public static void refresh() {
        
        boolean profit = Config.get(BooleanKey.HUD_PROFIT_ENABLED);
        boolean pricePerItem = Config.get(BooleanKey.PER_ITEM);
        boolean collection = Config.get(BooleanKey.HUD_COLLECTION_ENABLED);
        boolean tt = Config.get(BooleanKey.ITEM_PRICE_TT);
        boolean sack = (Config.get(BooleanKey.TRACK_SACK) && profit) || collection; 

        if (!PriceServiceManager.isInitialized() && (profit || collection || tt))  {
            PriceServiceManager.initialize();
        }
        
        CollectionTracker.initIfNeeded(collection); 
        SackDropParser.setTracking(sack);
        ProfitTracker.setConfig(profit, sack, pricePerItem, tt);
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

    /**
     * Format a price by grouping digits
     */
    public static String formatPrice(double price) {
        return PRICE_FORMAT.format(price);
    }
}
