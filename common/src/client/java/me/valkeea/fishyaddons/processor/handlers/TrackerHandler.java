package me.valkeea.fishyaddons.processor.handlers;

import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.tracker.profit.ProfitTracker;

public class TrackerHandler implements ChatHandler {
    
    @Override
    public int getPriority() {
        return 60;
    }
    
    @Override
    public String getHandlerName() {
        return "Tracker";
    }
    
    @Override
    public boolean shouldHandle(ChatMessageContext context) {
        return context.isSystemMessage();
    }
    
    @Override
    public ChatHandlerResult handle(ChatMessageContext context) {
        try {
            String unfilteredText = context.getLowerCleanString();
            var originalText = context.getOriginalText();
            if (ProfitTracker.handleChat(unfilteredText, originalText)) return ChatHandlerResult.STOP;
            return ChatHandlerResult.CONTINUE;

        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in Tracker handler: " + e.getMessage());
            e.printStackTrace();
            return ChatHandlerResult.SKIP;
        }
    }
    
    @Override
    public boolean isEnabled() {
        return ProfitTracker.isEnabled();
    }
}
