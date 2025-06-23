package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;

import java.util.Map;

public class ChatReplacement {
    private ChatReplacement() {}
    private static Map<String, String> cachedReplacements = Map.of();

    public static void refreshCache() {
        cachedReplacements = Map.copyOf(FishyConfig.getChatReplacements());
    }

    public static String applyReplacements(String message) {
        String result = message;
        for (Map.Entry<String, String> entry : cachedReplacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
