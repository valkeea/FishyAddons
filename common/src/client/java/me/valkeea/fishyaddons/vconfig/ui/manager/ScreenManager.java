package me.valkeea.fishyaddons.vconfig.ui.manager;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.tool.GuiScheduler;
import me.valkeea.fishyaddons.vconfig.ui.screen.VCScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

public class ScreenManager {
    
    // Cached screen instance
    private static VCScreen instance = null;
    
    // Flag to track if screen needs reinitialization
    private static boolean stale = false;
    
    /**
     * Get the config screen instance, creating it if necessary.
     * Returns cached instance if available and valid.
     * 
     * @return The VCScreen instance
     */
    public static VCScreen getOrCreateConfigScreen() {
        
        if (instance != null) {
            if (!stale) {
                return instance;
            } else { 
                instance.refreshLayout();
                stale = false;
                return instance;
            }
        }
        
        instance = new VCScreen();
        stale = false;
        return instance;
    }
    
    /**
     * Schedule opening VCScreen, using cached instance if available.
     */
    public static void openConfigScreen() {
        GuiScheduler.scheduleGui(getOrCreateConfigScreen());
    }
    
    public static void invalidateCache() {
        instance = null;
        stale = false;
    }

    public static void markStale() {
        stale = true;
    }
    
    public static boolean hasCachedScreen() {
        return instance != null;
    }
    
    /**
     * Get the cached screen without creating a new one.
     * Returns null if no screen is cached.
     * 
     * @return The cached VCScreen or null
     */
    public static @Nullable VCScreen getCachedScreen() {
        return instance;
    }

    /**
     * Get the cached screen if available, otherwise returns current screen.
     */
    public static Screen getConfigOrCurrent() {
        return isConfigScreenActive()
            ? getCachedScreen()
            : MinecraftClient.getInstance().currentScreen;
    }
    
    /**
     * Check if the current screen is the config screen.
     */
    public static boolean isConfigScreenActive() {
        var mc = MinecraftClient.getInstance();
        return mc.currentScreen instanceof VCScreen;
    }
    
    /**
     * Preserve current screen state before making changes.
     */
    public static void preserveCurrentState() {
        if (instance != null) {
            instance.preserveState();
        }
    }

    /**
     * Refresh the current screen's layout without closing it.
     * Only works if the config screen is currently active.
     */
    public static void refreshCurrentScreen() {
        if (instance != null && isConfigScreenActive()) {
            instance.refreshLayout();
        }
    }

    /**
     * Open the provided screen while preserving the current config screen state.
     */
    public static void navigateConfigScreen(Screen newScreen) {
        preserveCurrentState();
        GuiScheduler.scheduleGui(newScreen);
    }

    /**
     * Attempt to navigate to a cached config screen
     * 
     * @param prevScreen The screen to open if the config screen is not active
     */
    public static void navigateOrParent(Screen prevScreen) {
        if (hasCachedScreen()) openConfigScreen();
        else GuiScheduler.scheduleGui(prevScreen);
    }

    private ScreenManager() {}
}
