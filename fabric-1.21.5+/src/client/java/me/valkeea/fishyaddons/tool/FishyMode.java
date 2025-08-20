package me.valkeea.fishyaddons.tool;

import me.valkeea.fishyaddons.config.FishyConfig;

public class FishyMode {
    private FishyMode() {}    
    private static String cachedTheme = null;
    private static int cachedColor = 0xFFE2CAE9;

    // Get theme from config to determine texture folders and text color
    public static void init() {
        cachedTheme = FishyConfig.getString("themeMode", "default");
        cachedColor = getThemeColor(cachedTheme);
        me.valkeea.fishyaddons.gui.VCVisuals.set(cachedColor);
    }

    private static int getThemeColor(String theme) {
        return switch (theme.toLowerCase()) {
            case "purple" -> 0xFFBB80DF;
            case "blue" -> 0xFFA2C8FF;
            case "white" -> 0xFFE5E5FF;
            case "green" -> 0xFFA2FFA2;
            default -> 0xFFE2CAE9;
        };
    }    

    public static int getThemeColor() {
        return cachedColor;
    }

    public static String getTheme() {
        return cachedTheme;
    }    

    public static void setTheme(String mode) {
        FishyConfig.setString("themeMode", mode);
        int color = getThemeColor(mode);
        setColor(color);
        me.valkeea.fishyaddons.gui.VCVisuals.set(color);
        cachedTheme = mode;
    }

    public static void setColor(int color) {
        cachedColor = color;
    }
}