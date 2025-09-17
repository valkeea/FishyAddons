package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.handler.ChatReplacement;
import me.valkeea.fishyaddons.handler.CommandAlias;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

public class ModifyChat {
    private ModifyChat() {}

    public static void init() {

        ClientSendMessageEvents.MODIFY_COMMAND.register(command -> {
            String remapped = CommandAlias.getActualCommand("/" + command.trim());
            if (remapped != null && remapped.startsWith("/")) {
                return remapped.substring(1);
            }
            return ChatReplacement.apply(command);
        });

        ClientSendMessageEvents.MODIFY_CHAT.register(ChatReplacement::apply);
    }
}
