package me.valkeea.fishyaddons.feature.qol.input;

import java.util.Map;

import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.config.impl.ShortcutsConfig;

@VCModule
public final class ChatReplacement {
    private ChatReplacement() {}
    private static boolean enabled = false;
    private static Map<String, String> config = Map.of();

    @VCListener(BooleanKey.CHAT_REPLACEMENTS)
    public static void refresh() {
        enabled = Config.get(BooleanKey.CHAT_REPLACEMENTS);
        config = Map.copyOf(ShortcutsConfig.getChat());
    }

    public static String apply(String message) {
        if (!enabled) return message;

        var result = message;
        for (var e : config.entrySet()) {
            if (ShortcutsConfig.isChatToggled(e.getKey()) && result.contains(e.getKey())) {
                result = result.replace(e.getKey(), e.getValue());
            }
        }
        
        return result;
    }
}
