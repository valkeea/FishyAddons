package me.valkeea.fishyaddons.tool;

import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.ui.widget.VCVisuals;

public class FishyMode {
    private FishyMode() {}
    private static final FishyMode INSTANCE = new FishyMode();

    private int cachedTheme = 0;
    private int cachedColor = 0xFFDABCEB;    

    public static void init() {
        getInstance();
        INSTANCE.cachedTheme = Config.get(IntKey.THEME_MODE);
        INSTANCE.cachedColor = getThemeColor(INSTANCE.cachedTheme);
        VCVisuals.set(INSTANCE.cachedColor);
    }

    private static FishyMode getInstance() {
        return INSTANCE;
    }

    private static int getThemeColor(int theme) {
        return switch (theme) {
            case 1 -> 0xFFC694E4;
            case 2 -> 0xFFA2C8FF;
            case 3 -> 0xFFE5E5FF;
            case 4 -> 0xFFA2FFA2;
            case 5 -> 0xFFFFBDC4;
            default -> 0xFFDABCEB;
        };
    }

    public static int getCmdColor() {
        int theme = INSTANCE.cachedTheme;
        return switch (theme) {
            case 1 -> 0xFF770EF8;
            case 2 -> 0xFFA2C8FF;
            case 3 -> 0xFFE5F2FF;
            case 4 -> 0xFFA2FFA2;
            case 5 -> 0xFFFFBDC4;
            default -> 0xFF7FFFD4;
        };
    }

    public static String themeName() {
        return switch (INSTANCE.cachedTheme) {
            case 1 -> "purple";
            case 2 -> "blue";
            case 3 -> "white";
            case 4 -> "green";
            case 5 -> "rose";
            default -> "default";
        };
    }

    public static int getThemeColor() {
        return INSTANCE.cachedColor;
    }

    public static int getTheme() {
        return INSTANCE.cachedTheme;
    }    

    public static void setTheme(int mode) {
        int color = getThemeColor(mode);
        setColor(color);
        VCVisuals.set(color);
        INSTANCE.cachedTheme = mode;
    }

    public static void setColor(int color) {
        INSTANCE.cachedColor = color;
    }
}
