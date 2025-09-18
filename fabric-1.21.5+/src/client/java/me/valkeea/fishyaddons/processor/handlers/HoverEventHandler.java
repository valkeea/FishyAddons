package me.valkeea.fishyaddons.processor.handlers;

import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.tracker.SackDropParser;
import me.valkeea.fishyaddons.tracker.TrackerUtils;

public class HoverEventHandler implements ChatHandler {
    
    @Override
    public int getPriority() {
        return 70;
    }
    
    @Override
    public String getHandlerName() {
        return "HoverEvents";
    }
    
    @Override
    public boolean shouldHandle(ChatMessageContext context) {
        if (!context.isSkyblockMessage() || context.isOverlay()) {
            return false;
        }

        String cleanText = context.getCleanText();
        return cleanText.startsWith("[Sacks] +") && cleanText.contains("items");
    }
    
    @Override
    public ChatHandlerResult handle(ChatMessageContext context) {
        try {
            if (TrackerUtils.checkForHoverEvents(context.getUnfilteredMessage())) {
                return ChatHandlerResult.STOP;
            }
            return ChatHandlerResult.CONTINUE;
        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in HoverEvents handler: " + e.getMessage());
            e.printStackTrace();
            return ChatHandlerResult.SKIP;
        }
    }
    
    @Override
    public boolean isEnabled() {
        return SackDropParser.isOn() || TrackerUtils.isEnabled();
    }
}
