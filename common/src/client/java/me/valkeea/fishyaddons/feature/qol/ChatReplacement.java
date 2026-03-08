package me.valkeea.fishyaddons.feature.qol;

import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;

public class ChatReplacement {
    private ChatReplacement() {}
    private static boolean enabled = false;
    private static Map<String, String> cached = Map.of();

    public static void refresh() {
        enabled = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.CHAT_REPLACEMENTS_ENABLED, false);
        cached = Map.copyOf(FishyConfig.getChatReplacements());
    }

    public static String apply(String message) {
        if (!enabled) return message;

        var result = message;
        for (Map.Entry<String, String> entry : cached.entrySet()) {
            if (FishyConfig.isChatReplacementToggled(entry.getKey()) && result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        
        return result;
    }
}
