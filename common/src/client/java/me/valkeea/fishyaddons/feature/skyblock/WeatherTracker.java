package me.valkeea.fishyaddons.feature.skyblock;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.ZoneUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WeatherTracker {
    private WeatherTracker() {}
    private static boolean lastRainState = false;
    private static boolean initialized = false;
    
    /**
     * Mixin hooks into the rain state change event
     * --> passed as !raining.
     */
    public static void onRainStateChange(boolean isRaining) {
        if (!FishyConfig.getState(Key.RAIN_NOTI, true)) { return; }
        if (!initialized && !ZoneUtils.checkDenOrPark()) { return; }

        if (lastRainState && !isRaining) {
            if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.getY() == 69) {
                Text message = Text.literal("Warning: rain tracking is disabled in water with no sky access.")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC);
                FishyNotis.send(message);
            } else {
                stopped();
            }

        } else if (!lastRainState && isRaining) {
            started();
        }

        lastRainState = isRaining;
    }

    public static boolean isRaining() {
        var client = MinecraftClient.getInstance();
        if (client.world != null) {
            return client.world.isRaining();
        }
        return false;
    }
    
    /**
     * Force check the current rain state and enable tracking
     */
    public static void track() {
        onRainStateChange(isRaining());
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
        if (me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.isDenOrPark()) {
            initialized = true;
            lastRainState = isRaining();
        } else {
            initialized = false;
            lastRainState = false;
        }
    }
    
    private static void stopped() {
        if (!FishyConfig.getState(Key.RAIN_NOTI, true)) {
            return;
        }
        
        FishyNotis.warn2("Rain has stopped!");
        PlaySound.rainAlarm();
    }
    
    private static void started() {
        if (!FishyConfig.getState(Key.RAIN_NOTI, true)) {
            return;
        }
        
        Text message = Text.literal("Rain has started!")
            .formatted(Formatting.GRAY, Formatting.ITALIC);
        FishyNotis.alert(message);
    }
}
