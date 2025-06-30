package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;

public class XpColor {
    private XpColor() {}
    private static int color = 0x80FF20;
    private static boolean isEnabled = false;
    private static boolean isOutlineEnabled = false;

    public static int get() { return color; }
    public static boolean isEnabled() { return isEnabled; }
    public static boolean isOutlineEnabled() { return isOutlineEnabled; }

    public static void refresh() {
        color = FishyConfig.getInt("xpColor");
        isEnabled = FishyConfig.getState("xpColorEnabled", false); 
        isOutlineEnabled = FishyConfig.getState("xpOutline", false);
    }

    public static void set(int newColor) {
        color = newColor;
        FishyConfig.setInt("xpColor", newColor);
        refresh();
    }

    public static void toggle() {
        FishyConfig.toggle("xpColorEnabled", false);
        refresh();
    }

    public static void toggleOutline() {
        FishyConfig.toggle("xpOutline", false);
        refresh();
    }
}