package me.valkeea.fishyaddons.processor.handlers;

import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.tracker.DianaStats;
import me.valkeea.fishyaddons.tracker.SlayerStats;
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
            
            if (DianaStats.getInstance().handleChat(context.getLowerCleanString())) return ChatHandlerResult.STOP;
            if (handleSlayer(context.getCleanString())) return ChatHandlerResult.STOP;            
            return ChatHandlerResult.CONTINUE;

        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in UserStats handler: " + e.getMessage());
            e.printStackTrace();
            return ChatHandlerResult.SKIP;
        }
    }

    private boolean handleSlayer(String message) {
        if (!SlayerStats.isEnabled()) return false;
        if (SlayerStats.handleSlayerCompletion(message)) return true;
        
        String trimmed = message.trim();
        if (trimmed.startsWith("» Slay ") && trimmed.contains("Combat XP worth of")) {
            return SlayerStats.getInstance().handleQuestDescription(trimmed);
        }
        
        return false;
    }
    
    @Override
    public boolean isEnabled() {
        return ScStats.isEnabled() || DianaStats.isEnabled() || SlayerStats.isEnabled();
    }
}
