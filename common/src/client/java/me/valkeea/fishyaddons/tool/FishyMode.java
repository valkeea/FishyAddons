package me.valkeea.fishyaddons.tool;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;

public class FishyMode {
    private FishyMode() {}
    private static final FishyMode INSTANCE = new FishyMode();
    private String cachedTheme = "default";
    private int cachedColor = 0xFFDABCEB;

    public static void init() {
        getInstance();
        INSTANCE.cachedTheme = FishyConfig.getString(me.valkeea.fishyaddons.config.Key.THEME_MODE, "default");
        INSTANCE.cachedColor = getThemeColor(INSTANCE.cachedTheme);
        me.valkeea.fishyaddons.ui.widget.VCVisuals.set(INSTANCE.cachedColor);
    }

    private static FishyMode getInstance() {
        return INSTANCE;
    }

    private static int getThemeColor(String theme) {
        return switch (theme.toLowerCase()) {
            case "purple" -> 0xFFC694E4;
            case "blue" -> 0xFFA2C8FF;
            case "white" -> 0xFFE5E5FF;
            case "green" -> 0xFFA2FFA2;
            default -> 0xFFDABCEB;
        };
    }

    public static int getCmdColor() {
        String theme = INSTANCE.cachedTheme;
        return switch (theme.toLowerCase()) {
            case "purple" -> 0xFF770EF8;
            case "blue" -> 0xFFA2C8FF;
            case "white" -> 0xFFE5F2FF;
            case "green" -> 0xFFA2FFA2;
            default -> 0xFF7FFFD4;
        };
    }

    public static int getThemeColor() {
        return INSTANCE.cachedColor;
    }

    public static String getTheme() {
        return INSTANCE.cachedTheme;
    }    

    public static void setTheme(String mode) {
        FishyConfig.setString(Key.THEME_MODE, mode);
        int color = getThemeColor(mode);
        setColor(color);
        me.valkeea.fishyaddons.ui.widget.VCVisuals.set(color);
        INSTANCE.cachedTheme = mode;
    }

    public static void setColor(int color) {
        INSTANCE.cachedColor = color;
    }
}
