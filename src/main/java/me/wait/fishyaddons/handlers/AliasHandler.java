package me.wait.fishyaddons.handlers;

import java.util.HashMap;
import java.util.Map;

import me.wait.fishyaddons.config.ConfigHandler;

public class AliasHandler {
    private static Map<String, String> cachedCommandAliases = new HashMap<>();

    public static void refreshCommandCache() {
        cachedCommandAliases.clear();
        cachedCommandAliases.putAll(ConfigHandler.getCommandAliases());
    }

    public static String getActualCommand(String input) {
    
        Map<String, String> aliases = ConfigHandler.getCommandAliases();
    
        String bestMatchAlias = null;
        String bestMatchCommand = null;
        int bestMatchLength = -1;
    
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            String alias = entry.getKey();
            String command = entry.getValue();
    
            if (!ConfigHandler.isCommandToggled(alias)) continue;
    
            if (input.equals(alias) || input.startsWith(alias + " ")) {
                // Pick the longest matching alias
                if (alias.length() > bestMatchLength) {
                    bestMatchAlias = alias;
                    bestMatchCommand = command;
                    bestMatchLength = alias.length();
                }
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
    
    public static boolean isAliasSystemEnabled() {
        return !ConfigHandler.getCommandAliases().isEmpty();
    }

    public static boolean hasAnyAliases() {
        return !ConfigHandler.getCommandAliases().isEmpty();
    }
    
    
}
