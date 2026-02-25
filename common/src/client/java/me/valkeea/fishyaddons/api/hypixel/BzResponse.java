package me.valkeea.fishyaddons.api.hypixel;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class BzResponse {
    
    private final Gson gson;
    
    public BzResponse() {
        this.gson = new Gson();
    }
    
    public BzResponse(Gson gson) {
        this.gson = gson;
    }
    
    public Map<String, PriceData> parse(String responseBody) {
        Map<String, PriceData> result = new HashMap<>();
        int failedProducts = 0;
        
        try {
            JsonObject root = gson.fromJson(responseBody, JsonObject.class);
            
            if (!root.has("success") || !root.get("success").getAsBoolean()) {
                System.err.println("Bazaar API returned success=false");
                return result;
            }
            
            JsonObject products = root.getAsJsonObject("products");
            
            for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
                String productId = entry.getKey();
                if (!parseProduct(entry, productId, result)) {
                    failedProducts++;
                }
            }
            
            if (failedProducts > 0) {
                System.err.println("Warning: Failed to parse " + failedProducts + " bazaar products (recovered " + result.size() + ")");
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing bazaar data: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    private boolean parseProduct(Map.Entry<String, JsonElement> entry, String productId, Map<String, PriceData> result) {
        try {
            JsonObject product = entry.getValue().getAsJsonObject();
            
            JsonObject quickStatus = product.getAsJsonObject("quick_status");
            if (quickStatus != null) {
                double buyPrice = quickStatus.has("buyPrice") 
                    ? quickStatus.get("buyPrice").getAsDouble() 
                    : 0.0;
                double sellPrice = quickStatus.has("sellPrice") 
                    ? quickStatus.get("sellPrice").getAsDouble() 
                    : 0.0;
                
                result.put(productId, new PriceData(buyPrice, sellPrice));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
