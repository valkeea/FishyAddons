package me.valkeea.fishyaddons.feature.skyblock.timer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.util.Identifier;

public class EffectRegistry {
    private static final Map<String, EffectConfig> CONFIGS = new HashMap<>();

    private EffectRegistry() {}

    public static class EffectConfig {
        public final String displayName;
        public final Identifier texture;
        public final boolean pauseOffline;

        public EffectConfig(String displayName, String textureName, boolean pauseOffline) {
            this.displayName = displayName;
            this.texture = Identifier.of("fishyaddons", "textures/hud/" + textureName + ".png");
            this.pauseOffline = pauseOffline;
        }
    }

    static {
        register("moby-duck: collector's edition", "Moby-Duck: Collector's Edition", "moby", true);
        register("refined dark cacao truffle", "Refined Dark Cacao Truffle", "truffle", true);
        register("filled rosewater flask", "Filled Rosewater Flask", "flask", false);
        register("re-heated gummy polar bear", "Re-heated Gummy Polar Bear", "gummy", true);
    }

    /**
     * Register a cooldown configuration.
     * @param matchKey The lowercase key to match against
     * @param displayName The display name shown to the user
     * @param textureName The texture file name without extension
     * @param pauseOffline Whether this cooldown should pause when offline
     */
    private static void register(String matchKey, String displayName, String textureName, boolean pauseOffline) {
        CONFIGS.put(matchKey.toLowerCase(Locale.ROOT), 
                    new EffectConfig(displayName, textureName, pauseOffline));
    }

    /**
     * Get the configuration for a cooldown item.
     * @param itemName The item name to look up (case-insensitive)
     * @return The configuration, or null if not registered
     */
    public static EffectConfig get(String itemName) {
        if (itemName == null) return null;
        return CONFIGS.get(itemName.toLowerCase(Locale.ROOT));
    }
}
