package me.valkeea.fishyaddons.processor.handlers;

import me.valkeea.fishyaddons.handler.ChatAlert;
import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;

public class ChatAlertHandler implements ChatHandler {
    
    @Override
    public int getPriority() {
        return 85;
    }
    
    @Override
    public String getHandlerName() {
        return "ChatAlert";
    }
    
    @Override
    public boolean shouldHandle(ChatMessageContext context) {
        return !context.isOverlay() && context.isSkyblockMessage();
    }
    
    @Override
    public ChatHandlerResult handle(ChatMessageContext context) {
        try {
            ChatAlert.handleMatch(context.getUnfilteredText());
            return ChatHandlerResult.CONTINUE;
        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in ChatAlert handler: " + e.getMessage());
            e.printStackTrace();
            return ChatHandlerResult.SKIP;
        }
    }
    
    @Override
    public boolean isEnabled() {
        return ChatAlert.isOn();
    }
}
