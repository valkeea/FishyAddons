package me.valkeea.fishyaddons.feature.qol;

import java.util.Map;

import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.config.impl.ShortcutsConfig;

@VCModule
public class ChatReplacement {
    private ChatReplacement() {}
    private static boolean enabled = false;
    private static Map<String, String> cached = Map.of();

    @VCListener(BooleanKey.CHAT_REPLACEMENTS)
    public static void refresh() {
        enabled = Config.get(BooleanKey.CHAT_REPLACEMENTS);
        cached = Map.copyOf(ShortcutsConfig.getChat());
    }

    public static String apply(String message) {
        if (!enabled) return message;

        var result = message;
        for (Map.Entry<String, String> entry : cached.entrySet()) {
            if (ShortcutsConfig.isChatToggled(entry.getKey()) && result.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        
        return result;
    }
}
