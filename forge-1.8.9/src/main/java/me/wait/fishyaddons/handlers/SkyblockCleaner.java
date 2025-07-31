package me.wait.fishyaddons.handlers;

import me.wait.fishyaddons.config.ConfigHandler;

public class SkyblockCleaner {
    private SkyblockCleaner() {}
    private static boolean hideHotspot = true;
    private static float hotspotDistance = 10.0f;

    public static boolean shouldHideHotspot() {
        return hideHotspot;
    }

    public static float getHotspotDistance() {
        return hotspotDistance;
    }

    public static void refresh() {
        hideHotspot = ConfigHandler.isHideHotspotEnabled();
    }
}