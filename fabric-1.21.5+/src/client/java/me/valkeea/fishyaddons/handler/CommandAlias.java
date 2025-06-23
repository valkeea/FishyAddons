package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import java.util.HashMap;
import java.util.Map;

public class CommandAlias {
    private CommandAlias() {}
    private static final Map<String, String> cachedCommandAliases = new HashMap<>();

    public static void refreshCache() {
        cachedCommandAliases.clear();
        cachedCommandAliases.putAll(FishyConfig.getCommandAliases());
    }

    public static String getActualCommand(String input) {
        Map<String, String> aliases = cachedCommandAliases;

        String bestMatchAlias = null;
        String bestMatchCommand = null;
        int bestMatchLength = -1;

        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            String alias = entry.getKey();
            String command = entry.getValue();

            if (!FishyConfig.isCommandToggled(alias)) continue;

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

