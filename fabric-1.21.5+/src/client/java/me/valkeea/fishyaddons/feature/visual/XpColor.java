package me.valkeea.fishyaddons.feature.visual;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;

public class XpColor {
    private XpColor() {}
    private static int color = 0x80FF20;
    private static boolean isEnabled = false;
    private static boolean isOutlineEnabled = false;

    public static int get() { return color; }
    public static boolean isEnabled() { return isEnabled; }
    public static boolean isOutlineEnabled() { return isOutlineEnabled; }

    public static void refresh() {
        color = FishyConfig.getInt(Key.XP_COLOR);
        isEnabled = FishyConfig.getState(Key.XP_COLOR_ON, false); 
        isOutlineEnabled = FishyConfig.getState(Key.XP_OUTLINE, false);
    }

    public static void set(int newColor) {
        color = newColor;
        FishyConfig.setInt(Key.XP_COLOR, newColor);
        refresh();
    }

    public static void toggle() {
        FishyConfig.toggle(Key.XP_COLOR_ON, false);
        refresh();
    }

    public static void toggleOutline() {
        FishyConfig.toggle(Key.XP_OUTLINE, false);
        refresh();
    }
}
