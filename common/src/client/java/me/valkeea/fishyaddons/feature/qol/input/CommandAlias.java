package me.valkeea.fishyaddons.feature.qol.input;

import java.util.Map;

import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.config.impl.ShortcutsConfig;

@VCModule
public final class CommandAlias {
    private CommandAlias() {}
    private static boolean enabled = false;
    private static Map<String, String> config = Map.of();

    @VCListener(BooleanKey.ALIASES)
    public static void refresh() {
        enabled = Config.get(BooleanKey.ALIASES);
        config = Map.copyOf(ShortcutsConfig.getAliases());
    }

    public static String apply(String input) {
        if (!enabled) return input;
        var aliases = config;

        String bestMatchIn = null;
        String bestMatchOut = null;
        int bestMatchLength = -1;

        for (var e : aliases.entrySet()) {
            var in = e.getKey();
            var out = e.getValue();

            if (!ShortcutsConfig.isAliasToggled(in)) continue;

            if ((input.equals(in) || input.startsWith(in + " ")) && in.length() > bestMatchLength) {
                bestMatchIn = in;
                bestMatchOut = out;
                bestMatchLength = in.length();
            }
        }

        if (bestMatchIn != null) {
            var remaining = input.length() > bestMatchIn.length()
                    ? input.substring(bestMatchIn.length()).trim()
                    : "";
            return remaining.isEmpty() ? bestMatchOut : bestMatchOut + " " + remaining;
        }

        return null;
    }

    public static boolean hasAnyAliases() {
        return !config.isEmpty();
    }
}
