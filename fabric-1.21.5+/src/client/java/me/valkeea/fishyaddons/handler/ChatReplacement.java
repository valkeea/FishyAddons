package me.valkeea.fishyaddons.handler;

import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;

public class ChatReplacement {
    private ChatReplacement() {}
    private static boolean enabled = false;
    private static Map<String, String> cachedReplacements = Map.of();

    public static void refresh() {
        enabled = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.CHAT_REPLACEMENTS_ENABLED, false);
        cachedReplacements = Map.copyOf(FishyConfig.getChatReplacements());
    }

    public static String applyReplacements(String message) {
        if (!enabled) return message;

        String result = message;
        for (Map.Entry<String, String> entry : cachedReplacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
