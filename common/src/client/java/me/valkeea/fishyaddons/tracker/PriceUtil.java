package me.valkeea.fishyaddons.tracker;

import java.text.DecimalFormat;
import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.api.hypixel.PriceServiceManager;
import me.valkeea.fishyaddons.tracker.collection.CollectionTracker;
import me.valkeea.fishyaddons.tracker.profit.ProfitTracker;
import me.valkeea.fishyaddons.tracker.profit.SackDropParser;
import me.valkeea.fishyaddons.util.text.FromText;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ItemLore;

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

    public record BookInfo(String name, Component styledName) {}
    
    /**
     * Extract item information from an enchanted book's lore
     * @return A BookInfo object containing the name and styled name of the book
     */
    public static @Nullable BookInfo getBookInfo(ItemLore lore) {

        for (Component i : lore.lines()) {
            if (i.toString().contains("Combinable in Anvil")) continue;

            var first = FromText.firstLiteral(i);
            if (first != null) {
                
                var plainName = first.getString();
                var numericName = toNumeric(plainName);
                var ultimateText = FromText.findNodeWithColor(i, ChatFormatting.LIGHT_PURPLE);

                Component styled;
                if (ultimateText != null) {
                    styled = ultimateText.copy();
                    numericName = numericName.contains("Ultimate") ? numericName : "ultimate " + numericName;
                } else {
                    styled = first.copy();
                }

                return new BookInfo("enchantment " + numericName, styled);
            }
        }

        return null;
    }

    private static String toNumeric(String plainName) {
        
        var parts = plainName.split(" ");
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
}
