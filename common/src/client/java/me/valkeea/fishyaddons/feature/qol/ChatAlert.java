package me.valkeea.fishyaddons.feature.qol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.valkeea.fishyaddons.api.skyblock.GameChat;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.FishyConfig.AlertData;
import me.valkeea.fishyaddons.hud.elements.simple.TitleDisplay;
import me.valkeea.fishyaddons.processor.BaseAnalysis;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.tool.PlayerPosition;
import me.valkeea.fishyaddons.util.ServerCommand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class ChatAlert {
    private ChatAlert() {}
    private static boolean enabled = false;
    
    private static final long TRIGGER_COOLDOWN_MS = 1000;
    private static final Map<String, Long> lastTriggerTime = new ConcurrentHashMap<>();

    public static void refresh() {
        enabled = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.CHAT_ALERTS_ENABLED, false);
        BaseAnalysis.clearCaches();
    }

    public static boolean isOn() {
        return enabled;
    }

    public static void handleMatch(ChatMessageContext context) {
        if (!enabled) return;

        long currentTime = System.currentTimeMillis();
        var analysis = context.getAnalysisResult();
        var firstMatch = analysis.getFirstAlertMatch();

        if (firstMatch != null) {
            
            String alertKey = firstMatch.getAlertKey();
            Long lastTrigger = lastTriggerTime.get(alertKey);

            if (lastTrigger == null || (currentTime - lastTrigger) > TRIGGER_COOLDOWN_MS) {
                executeAlert(firstMatch.getAlertData());
                lastTriggerTime.put(alertKey, currentTime);
                
                cleanupOldTriggers(currentTime);
            }
        }
    }

    public static void executeAlert(AlertData data) {
        var client = MinecraftClient.getInstance();

        if (data.getMsg() != null && !data.getMsg().isBlank() &&
            client.player != null) {
            handleMsg(data, client);
        }

        if (data.getOnscreen() != null && !data.getOnscreen().isBlank() &&
            client.inGameHud != null) {
            TitleDisplay.setTitle(data.getOnscreen(), data.getColor());
        }

        if (data.getSoundId() != null && !data.getSoundId().isBlank() &&
            client.player != null) {
            try {
                var id = Identifier.tryParse(data.getSoundId());
                if (id != null) {
                    PlaySound.dynamic(id.toString(),
                    data.getVolume(), 1.0F, false);
                }
            } catch (Exception ignored) {
                // Ignore sound playback errors
            }
        }
    }    

    private static void handleMsg(AlertData data, MinecraftClient client) {
        String message = data.getMsg().trim();
        boolean isInParty = GameChat.isInParty() || GameChat.partyToggled();

        if (message.isBlank()) return;

        if (message.contains("<pos>")) {
            String coords = PlayerPosition.getCoordsString(client);
            message = message.replace("<pos>", coords);
        }

        if (message.startsWith("/")) {
            message = message.substring(1);
            ServerCommand.send(message);
        } else if (isInParty) {
            ServerCommand.send("pc " + message);
        }
    }

    private static void cleanupOldTriggers(long currentTime) {
        lastTriggerTime.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > (TRIGGER_COOLDOWN_MS * 10));
    }    
}
