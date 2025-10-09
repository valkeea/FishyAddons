package me.valkeea.fishyaddons.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.valkeea.fishyaddons.api.skyblock.GameChat;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.FishyConfig.AlertData;
import me.valkeea.fishyaddons.hud.TitleDisplay;
import me.valkeea.fishyaddons.processor.MessageAnalysis;
import me.valkeea.fishyaddons.processor.MessageAnalysis.AlertMatch;
import me.valkeea.fishyaddons.processor.SharedMessageDetector;
import me.valkeea.fishyaddons.util.SoundUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class ChatAlert {
    private ChatAlert() {}
    private static boolean enabled = false;
    
    private static final long TRIGGER_COOLDOWN_MS = 1000;
    private static final long COMMAND_COOLDOWN_MS = 2000;
    private static final Map<String, Long> lastTriggerTime = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastCommandTime = new ConcurrentHashMap<>();

    public static void refresh() {
        enabled = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.CHAT_ALERTS_ENABLED, false);
        SharedMessageDetector.clearCaches();
    }

    public static boolean isOn() {
        return enabled;
    }

    public static void handleMatch(String s) {
        if (!enabled) return;
        
        long currentTime = System.currentTimeMillis();
        MessageAnalysis analysis = SharedMessageDetector.analyzeMessage(s);
        
        if (analysis.hasAlertMatches()) {
            AlertMatch firstMatch = analysis.getAlertMatches().get(0);
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
        MinecraftClient client = MinecraftClient.getInstance();

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
                Identifier id = Identifier.tryParse(data.getSoundId());
                if (id != null) {
                    SoundUtil.playDynamicSound(id.toString(),
                    data.getVolume(), 1.0F);
                }
            } catch (Exception ignored) {
                // Ignore sound playback errors
            }
        }
    }    

    private static boolean isValid(String command) {
        return command != null && !command.trim().isEmpty();
    }

    /**
     * Check if enough time has passed since the last command execution.
     */
    private static boolean canExecuteCommand() {
        Long lastExecution = lastCommandTime.get("global");
        if (lastExecution == null) {
            return true;
        }
        return (System.currentTimeMillis() - lastExecution) > COMMAND_COOLDOWN_MS;
    }

    private static void handleMsg(AlertData data, MinecraftClient client) {
        String message = data.getMsg().trim();

        if (message.startsWith("/")) {
            String command = message.substring(1);

            if (isValid(command) && canExecuteCommand() && GameChat.partyToggled()) {
                client.player.networkHandler.sendChatCommand(command);
                lastCommandTime.put("global", System.currentTimeMillis());
            }

        } else {
            client.player.networkHandler.sendChatMessage(message);
        }
    }

    private static void cleanupOldTriggers(long currentTime) {
        lastTriggerTime.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > (TRIGGER_COOLDOWN_MS * 10));
    }    
}