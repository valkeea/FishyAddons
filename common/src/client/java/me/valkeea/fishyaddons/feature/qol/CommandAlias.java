package me.valkeea.fishyaddons.feature.qol;

import java.util.HashMap;
import java.util.Map;

import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.config.impl.ShortcutsConfig;

@VCModule
public class CommandAlias {
    private CommandAlias() {}
    private static boolean enabled = false;
    private static final Map<String, String> cachedCommandAliases = new HashMap<>();

    @VCListener(BooleanKey.ALIASES)
    public static void refresh() {
        enabled = Config.get(BooleanKey.ALIASES);
        cachedCommandAliases.clear();
        cachedCommandAliases.putAll(ShortcutsConfig.getAliases());
    }

    public static String getActualCommand(String input) {
        if (!enabled) return input;
        Map<String, String> aliases = cachedCommandAliases;

        String bestMatchAlias = null;
        String bestMatchCommand = null;
        int bestMatchLength = -1;

        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            String alias = entry.getKey();
            String command = entry.getValue();

            if (!ShortcutsConfig.isAliasToggled(alias)) continue;

            if ((input.equals(alias) || input.startsWith(alias + " ")) && alias.length() > bestMatchLength) {
                bestMatchAlias = alias;
                bestMatchCommand = command;
                bestMatchLength = alias.length();
            }
        }

        if (bestMatchAlias != null) {
            String remaining = input.length() > bestMatchAlias.length()
                    ? input.substring(bestMatchAlias.length()).trim()
                    : "";
            return remaining.isEmpty() ? bestMatchCommand : bestMatchCommand + " " + remaining;
        }

        return null;
    }

    public static boolean hasAnyAliases() {
        return !cachedCommandAliases.isEmpty();
    }
}
