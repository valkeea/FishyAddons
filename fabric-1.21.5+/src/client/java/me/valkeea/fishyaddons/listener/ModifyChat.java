package me.valkeea.fishyaddons.listener;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import me.valkeea.fishyaddons.handler.CommandAlias;
import me.valkeea.fishyaddons.handler.ChatReplacement;

public class ModifyChat {
    private ModifyChat() {}

    public static void init() {
        ClientSendMessageEvents.MODIFY_COMMAND.register(command -> {
            String remapped = CommandAlias.getActualCommand("/" + command.trim());
            if (remapped != null && remapped.startsWith("/")) {
                return remapped.substring(1);
            }
            return command;
        });

        ClientSendMessageEvents.MODIFY_CHAT.register(message -> 
            ChatReplacement.applyReplacements(message)
        );
    }
}
