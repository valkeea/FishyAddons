package me.valkeea.fishyaddons.api.hypixel;

public record PriceData(double buyPrice, double sellPrice, long timestamp) {
    
    public PriceData(double buyPrice, double sellPrice) {
        this(buyPrice, sellPrice, System.currentTimeMillis());
    }
    
    /** 
     * Get price based on type string ("buyPrice" or "sellPrice")
     */
    public double getPrice(String priceType) {
        return "buyPrice".equals(priceType) ? buyPrice : sellPrice;
    }
    
    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - timestamp > ttlMillis;
    }
    
    @Override
    public String toString() {
        return String.format("PriceData{buy=%.2f, sell=%.2f, age=%dms}", 
            buyPrice, sellPrice, System.currentTimeMillis() - timestamp);
    }
}
