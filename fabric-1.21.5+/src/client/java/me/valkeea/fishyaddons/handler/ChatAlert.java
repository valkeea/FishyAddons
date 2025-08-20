package me.valkeea.fishyaddons.handler;

import java.util.HashMap;
import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.FishyConfig.AlertData;
import me.valkeea.fishyaddons.hud.TitleDisplay;
import me.valkeea.fishyaddons.util.SoundUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class ChatAlert {
    private static final Map<String, AlertData> alertCache = new HashMap<>();

    public static void refresh() {
        alertCache.clear();
        alertCache.putAll(FishyConfig.getChatAlerts());
    }

    public static void handleMatch(String message) {
        // Ignore status bar messages
        if (message.contains("❤") || message.contains("❈") || message.contains("✎") || message.contains("ʬ")) {
            return;
        }
        for (Map.Entry<String, AlertData> entry : alertCache.entrySet()) {
            String key = entry.getKey();
            AlertData data = entry.getValue();
            if (data == null || !data.isToggled()) continue;
            if (message.contains(key)) {
                executeAlert(data);
                break;
            }
        }
    }

    public static void executeAlert(AlertData data) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (data.getMsg() != null && !data.getMsg().isBlank() && client.player != null) {
            client.player.networkHandler.sendChatMessage(data.getMsg());
        }

        if (data.getOnscreen() != null && !data.getOnscreen().isBlank() && client.inGameHud != null) {
            TitleDisplay.setTitle(data.getOnscreen(), data.getColor());
        }

        if (data.getSoundId() != null && !data.getSoundId().isBlank() && client.player != null) {
            try {
                Identifier id = Identifier.tryParse(data.getSoundId());
                if (id != null) {
                    SoundUtil.playDynamicSound(id.toString(), data.getVolume(), 1.0F);
                }
            } catch (Exception ignored) {}
        }
    }
}