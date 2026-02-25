package me.valkeea.fishyaddons.api.hypixel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record TieredPriceData(Map<String, Double> pricesByRarity, long timestamp) {
    
    public TieredPriceData(Map<String, Double> pricesByRarity) {
        this(new HashMap<>(pricesByRarity), System.currentTimeMillis());
    }
    
    public TieredPriceData {
        pricesByRarity = Collections.unmodifiableMap(new HashMap<>(pricesByRarity));
    }
    
    /**
     * Get the price for a specific rarity
     */
    public double getPrice(String rarity) {
        return pricesByRarity.getOrDefault(rarity.toUpperCase(), 0.0);
    }
    
    /**
     * Check if this data has a price for the given rarity
     */
    public boolean hasRarity(String rarity) {
        return pricesByRarity.containsKey(rarity.toUpperCase());
    }
    
    public Map<String, Double> getAllPrices() {
        return pricesByRarity;
    }
    
    public double getLowestPrice() {
        return pricesByRarity.values().stream()
            .mapToDouble(Double::doubleValue)
            .min()
            .orElse(0.0);
    }
    
    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - timestamp > ttlMillis;
    }
    
    @Override
    public String toString() {
        return String.format("TieredPriceData{rarities=%s, age=%dms}", 
            pricesByRarity.keySet(), System.currentTimeMillis() - timestamp);
    }
}
