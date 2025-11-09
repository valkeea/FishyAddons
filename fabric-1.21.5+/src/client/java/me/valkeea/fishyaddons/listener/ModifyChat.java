package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.api.skyblock.GameChat;
import me.valkeea.fishyaddons.feature.qol.ChatReplacement;
import me.valkeea.fishyaddons.feature.qol.CommandAlias;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

public class ModifyChat {
    private ModifyChat() {}

    public static void init() {

        ClientSendMessageEvents.MODIFY_COMMAND.register(command -> {
            trackChatMode(command);
            
            String remapped = CommandAlias.getActualCommand("/" + command.trim());
            if (remapped != null && remapped.startsWith("/")) {
                return remapped.substring(1);
            }
            return ChatReplacement.apply(command);
        });

        ClientSendMessageEvents.MODIFY_CHAT.register(ChatReplacement::apply);
    }
    
    /**
     * Tracks changes to chat mode when Hypixel chat commands are used.
     */
    private static void trackChatMode(String command) {
        if (command == null) return;
        
        String[] parts = command.trim().split("\\s+");
        if (parts.length >= 2 && "chat".equalsIgnoreCase(parts[0])) {
            String chatMode = parts[1].toLowerCase();
            GameChat.changedChannel(chatMode);
        }
    }
}
