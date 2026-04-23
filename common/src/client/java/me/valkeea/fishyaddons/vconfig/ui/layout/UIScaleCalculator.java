package me.valkeea.fishyaddons.vconfig.ui.layout;

import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import net.minecraft.client.MinecraftClient;

public final class UIScaleCalculator {
    
    private static final float ABSOLUTE_MIN_SCALE = 1.0f;
    private static final float ABSOLUTE_MAX_SCALE = 2.0f;
    
    /**
     * Calculate UI scale for VCScreen and related interfaces.
     * 
     * @param screenWidth The screen width in pixels
     * @return The relative UI scale based on user preference and screen size, clamped to absolute bounds
     */
    public static float calculateUIScale(int screenWidth) {
        float w = screenWidth;   
        double userPreference = Config.get(DoubleKey.MOD_UI_SCALE);
        float uiPanelW = Dimensions.TOTAL_LIST_WIDTH;
        float minRatio = 2.5f;
        
        // Calculate scale bounds
        // At pref 1.0: visualWidth = screenWidth / 2.5 → scale = screenWidth / (2.5 * uiPanelW)
        // At pref 2.0: visualWidth = screenWidth → scale = screenWidth / uiPanelW
        float minScale = w / (minRatio * uiPanelW);
        float maxScale = w / uiPanelW;
        
        // Linearly interpolate between min and max scale based on user preference
        float t = Math.clamp((float)userPreference, ABSOLUTE_MIN_SCALE, ABSOLUTE_MAX_SCALE) - ABSOLUTE_MIN_SCALE;
        return minScale + t * (maxScale - minScale);
    }
    
    /**
     * Calculate UI scale using current window dimensions.
     * 
     * @return The calculated UI scale
     */
    public static float calculateUIScale() {
        var mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) {
            return (float) Math.clamp(Config.get(DoubleKey.MOD_UI_SCALE), ABSOLUTE_MIN_SCALE, ABSOLUTE_MAX_SCALE);
        }
        int width = mc.getWindow().getFramebufferWidth();
        return calculateUIScale(width);
    }

    /**
     * Calculate UI scale for legacy screens that used to use direct config access.
     * 
     * @return The calculated UI scale, clamped to provided bounds
     */
    public static float calculateUIScaleLegacy() {
        float calculatedScale = calculateUIScale();
        float legacyMin = 0.7f;
        float legacyMax = 1.3f;
        float mul = legacyMin / ABSOLUTE_MIN_SCALE;
        return Math.clamp(calculatedScale * mul, legacyMin, legacyMax);
    }

    private UIScaleCalculator() {}
}
