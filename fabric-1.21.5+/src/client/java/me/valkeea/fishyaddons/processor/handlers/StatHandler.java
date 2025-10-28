package me.valkeea.fishyaddons.processor.handlers;

import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.tracker.DianaStats;
import me.valkeea.fishyaddons.tracker.fishing.ScStats;

public class StatHandler implements ChatHandler {
    
    @Override
    public int getPriority() {
        return 65;
    }
    
    @Override
    public String getHandlerName() {
        return "StatHandler";
    }
    
    @Override
    public boolean shouldHandle(ChatMessageContext context) {
        return context.isSkyblockMessage() && context.isSystemMessage();
    }
    
    @Override
    public ChatHandlerResult handle(ChatMessageContext context) {

        try {
            if (ScStats.getInstance().handleMatch(context.getCleanText())) {
                return ChatHandlerResult.STOP;
            }
            if (DianaStats.getInstance().handleChat(context.getLowercaseText())) {
                return ChatHandlerResult.STOP;
            }
            return ChatHandlerResult.CONTINUE;

        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in UserStats handler: " + e.getMessage());
            e.printStackTrace();
            return ChatHandlerResult.SKIP;
        }
    }  
    
    @Override
    public boolean isEnabled() {
        return ScStats.isEnabled() || DianaStats.isEnabled();
    }
}