package me.valkeea.fishyaddons.processor.handlers;

import me.valkeea.fishyaddons.handler.NcpDialogue;
import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;

public class ClickEventHandler implements ChatHandler {
    
    @Override
    public int getPriority() {
        return 35;
    }
    
    @Override
    public String getHandlerName() {
        return "ClickEvents";
    }
    
    @Override
    public boolean shouldHandle(ChatMessageContext context) {
        return context.isSkyblockMessage();
    }
    
    @Override
    public ChatHandlerResult handle(ChatMessageContext context) {
        String clean = context.getUnfilteredCleanLowercaseText();
        
        try {
            if ((clean.startsWith("select an option:") || clean.startsWith("accept the trapper's task to hunt the animal?")) && 
                NcpDialogue.checkForCommands(context.getUnfilteredMessage())) {
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
        return NcpDialogue.enabled();
    }
}
