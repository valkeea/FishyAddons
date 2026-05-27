package me.valkeea.fishyaddons.api.hypixel;

import org.jetbrains.annotations.Nullable;

/**
 * Global singleton manager for the PriceService.
 */
public class PriceServiceManager {
    
    @Nullable
    private static PriceService instance = null;
    private static boolean initialized = false;
    
    private PriceServiceManager() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Initialize the price service.
     */
    public static synchronized void initialize() {
        if (!initialized) {
            instance = new PriceService();
            initialized = true;
        }
    }
    
    /**
     * Get the singleton service instance.
     * 
     * @return The service instance
     * @throws IllegalStateException if not initialized
     */
    public static PriceService getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "PriceService not initialized."
            );
        }
        return instance;
    }
    
    /**
     * Check if the service is initialized.
     * 
     * @return true if initialized and ready to use
     */
    public static boolean isInitialized() {
        return initialized && instance != null;
    }

    /**
     * Run this action if the service is initialized, otherwise do nothing.
     */
    public static void ifInitialized(Runnable action) {
        if (isInitialized()) {
            action.run();
        }
    }    
    
    /**
     * Get the service instance if initialized, or null if not.
     * 
     * @return The service instance, or null
     */
    @Nullable
    public static PriceService getInstanceOrNull() {
        return instance;
    }
    
    /**
     * Shutdown and clean up resources.
     */
    public static synchronized void shutdown() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
            initialized = false;
        }
    }
    
    /**
     * Shutdown and reinitialize.
     */
    public static synchronized void restart() {
        shutdown();
        initialize();
    } 
}
