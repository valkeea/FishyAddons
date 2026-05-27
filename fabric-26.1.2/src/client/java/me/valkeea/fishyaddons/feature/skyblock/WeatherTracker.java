package me.valkeea.fishyaddons.feature.skyblock;

import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.ZoneUtils;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class WeatherTracker {
    private static final int CAVE_Y_LEVEL = 69;
    
    private static boolean lastRainState = false;
    private static boolean initialized = false;
    
    public static void onRainLevelChange(boolean isRaining) {
        if (!Config.get(BooleanKey.RAIN_NOTI) || (!initialized && !ZoneUtils.checkRainArea())) return;

        if (lastRainState && !isRaining) {
            var mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getY() == CAVE_Y_LEVEL) warnNoSky();
            else rainStopped();

        } else if (!lastRainState && isRaining) rainStarted();

        lastRainState = isRaining;
    }

    public static boolean isRaining() {
        var mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.isRaining();
        }
        return false;
    }
    
    /**
     * Force check the current rain state and enable tracking
     */
    public static void track() {
        onRainLevelChange(isRaining());
        if (!initialized) {
            lastRainState = isRaining();
            initialized = true;
        }
    }
    
    /**
     * Reset the tracker
     */
    public static void reset() {
        initialized = false;
        lastRainState = false;
        shouldTrack();
    }

    /**
     * Track if in den or park, or if manually set with /fa rain track
     * This will reinitialize the tracker
     */
    public static void shouldTrack() {
        if (me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.isRainArea()) {
            initialized = true;
            lastRainState = isRaining();
        } else {
            initialized = false;
            lastRainState = false;
        }
    }
    
    private static void rainStopped() {
        FishyNotis.warn2("Rain has stopped!");
        PlaySound.rainAlarm();
    }
    
    private static void rainStarted() {
        var message = Component.literal("Rain has started!")
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
        FishyNotis.alert(message);
    }

    private static void warnNoSky() {
        var message = Component.literal("Warning: rain tracking is disabled in water with no sky access.")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        FishyNotis.send(message);
    }

    private WeatherTracker() {}    
}
