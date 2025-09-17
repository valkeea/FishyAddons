package me.valkeea.fishyaddons.tool;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;

public class FishyMode {
    private FishyMode() {}    
    private static String cachedTheme = null;
    private static int cachedColor = 0xFFE2CAE9;

    public static void init() {
        cachedTheme = FishyConfig.getString(me.valkeea.fishyaddons.config.Key.THEME_MODE, "default");
        cachedColor = getThemeColor(cachedTheme);
        me.valkeea.fishyaddons.ui.widget.VCVisuals.set(cachedColor);
    }

    private static int getThemeColor(String theme) {
        return switch (theme.toLowerCase()) {
            case "purple" -> 0xFFC694E4;
            case "blue" -> 0xFFA2C8FF;
            case "white" -> 0xFFE5E5FF;
            case "green" -> 0xFFA2FFA2;
            default -> 0xFFE2CAE9;
        };
    }

    public static int getCmdColor() {
        String theme = cachedTheme;
        return switch (theme.toLowerCase()) {
            case "purple" -> 0xFF770EF8;
            case "blue" -> 0xFFA2C8FF;
            case "white" -> 0xFFE5F2FF;
            case "green" -> 0xFFA2FFA2;
            default -> 0xFF14FFC2;
        };
    }

    public static int getThemeColor() {
        return cachedColor;
    }

    public static String getTheme() {
        return cachedTheme;
    }    

    public static void setTheme(String mode) {
        FishyConfig.setString(Key.THEME_MODE, mode);
        int color = getThemeColor(mode);
        setColor(color);
        me.valkeea.fishyaddons.ui.widget.VCVisuals.set(color);
        cachedTheme = mode;
    }

    public static void setColor(int color) {
        cachedColor = color;
    }
}