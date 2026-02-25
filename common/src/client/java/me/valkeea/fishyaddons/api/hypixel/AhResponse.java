package me.valkeea.fishyaddons.api.hypixel;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AhResponse {
    
    private final Gson gson;
    
    public AhResponse() {
        this.gson = new Gson();
    }
    
    public AhResponse(Gson gson) {
        this.gson = gson;
    }
    
    public ParseResult parsePage(String responseBody) {
        ParseResult result = new ParseResult();
        int failedAuctions = 0;
        
        try {
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            
            if (!root.has("success") || !root.get("success").getAsBoolean()) {
                System.err.println("Auction API returned success=false");
                return result;
            }
            
            result.totalPages = root.get("totalPages").getAsInt();
            JsonArray auctions = root.getAsJsonArray("auctions");
            
            for (JsonElement auctionElement : auctions) {
                if (!processAuction(auctionElement, result)) {
                    failedAuctions++;
                }
            }
            
            if (failedAuctions > 0) {
                System.err.println("Warning: Failed to parse " + failedAuctions + " auctions (recovered " + result.itemsProcessed + ")");
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing auction data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    private boolean processAuction(JsonElement auctionElement, ParseResult result) {
        try {
            JsonObject auction = auctionElement.getAsJsonObject();
            
            if (!auction.has("bin") || !auction.get("bin").getAsBoolean()) {
                return true;
            }
            
            String itemName = auction.get("item_name").getAsString();
            double price = auction.get("starting_bid").getAsDouble();
            String cleanedName = ItemNameMapper.cleanDisplayName(itemName);
            
            String rarity = auction.has("tier") 
                ? auction.get("tier").getAsString().toUpperCase() 
                : "COMMON";
            
            String baseName = cleanedName // Strip level, not needed for drop tracking
                .replaceAll("\\[Lvl \\d+\\]\\s*", "")
                .trim()
                .toLowerCase();
            
            result.itemPrices.putIfAbsent(baseName, new HashMap<>());
            Map<String, Double> rarityPrices = result.itemPrices.get(baseName);
            
            // Keep lowest price for each rarity
            rarityPrices.compute(rarity, (k, v) -> v == null || price < v ? price : v);
            
            result.itemsProcessed++;
            return true;
        } catch (Exception e) {
            // Skip and continue with others
            return false;
        }
    }
    
    /**
     * Convert the accumulated price data into TieredPriceData.
     * 
     * @param itemPrices Map of item name to (rarity -> price)
     * @return Map of item name to TieredPriceData
     */
    public Map<String, TieredPriceData> buildTieredCache(Map<String, Map<String, Double>> itemPrices) {
        Map<String, TieredPriceData> cache = new HashMap<>();
        
        for (var entry : itemPrices.entrySet()) {
            String itemName = entry.getKey();
            Map<String, Double> rarityPrices = entry.getValue();
            
            var tieredData = new TieredPriceData(rarityPrices);
            cache.put(itemName, tieredData);
        }
        
        return cache;
    }
    
    public static class ParseResult {
        public final Map<String, Map<String, Double>> itemPrices = new HashMap<>();
        private int totalPages = 0;
        private int itemsProcessed = 0;

        public int getTotalPages() {
            return totalPages;
        }
        
        public void merge(ParseResult other) {
            this.totalPages = Math.max(this.totalPages, other.totalPages);
            this.itemsProcessed += other.itemsProcessed;
            
            for (var entry : other.itemPrices.entrySet()) {
                String itemName = entry.getKey();
                Map<String, Double> otherRarityPrices = entry.getValue();
                
                this.itemPrices.putIfAbsent(itemName, new HashMap<>());
                Map<String, Double> thisRarityPrices = this.itemPrices.get(itemName);
                
                for (var rarityEntry : otherRarityPrices.entrySet()) {
                    String rarity = rarityEntry.getKey();
                    double price = rarityEntry.getValue();
                    
                    thisRarityPrices.compute(rarity, (k, v) -> v == null || price < v ? price : v);
                }
            }
        }
    }
}
