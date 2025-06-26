package me.valkeea.fishyaddons.tool;

import me.valkeea.fishyaddons.config.FishyConfig;

public class FishyMode {
    private static String cachedTheme = null;

    private FishyMode() {}

    // Get theme from config to determine texture folders
    public static String getTheme() {
        if (cachedTheme == null) {
            cachedTheme = FishyConfig.getString("themeMode", "default");
        }
        return cachedTheme;
    }

    public static void setTheme(String mode) {
        FishyConfig.setString("themeMode", mode);
        cachedTheme = mode;
    }

    public static void invalidate() {
        cachedTheme = null;
    }
}