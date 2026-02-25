package me.valkeea.fishyaddons.api.hypixel;

/**
 * Indicates where a price came from.
 */
public enum PriceSource {
    /**
     * Price found in bazaar cache
     */
    BAZAAR,
    
    /**
     * Price found in auction (BIN) cache
     */
    AUCTION,
    
    /**
     * Price not found in any cache
     */
    NONE
}
