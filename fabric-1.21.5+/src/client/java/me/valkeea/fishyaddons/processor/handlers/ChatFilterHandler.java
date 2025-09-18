package me.valkeea.fishyaddons.processor.handlers;

import me.valkeea.fishyaddons.handler.ChatFilter;
import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import net.minecraft.text.Text;

public class ChatFilterHandler implements ChatHandler {
    
    @Override
    public int getPriority() {
        return 95;
    }
    
    @Override
    public String getHandlerName() {
        return "ChatFilter";
    }
    
    @Override
    public boolean shouldHandle(ChatMessageContext context) {
        return !context.isOverlay();
    }
    
    @Override
    public ChatHandlerResult handle(ChatMessageContext context) {
        try {
            Text originalMessage = context.getOriginalMessage();
            Text filteredMessage = ChatFilter.applyFilters(originalMessage);
            
            if (!filteredMessage.equals(originalMessage)) {
                ChatMessageContext newContext = new ChatMessageContext(filteredMessage, context.getUnfilteredMessage(), context.isOverlay());
                return ChatHandlerResult.modifyWith(newContext, "Message filtered");
            }
            return ChatHandlerResult.CONTINUE;
        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in ChatFilter handler: " + e.getMessage());
            e.printStackTrace();
            return ChatHandlerResult.SKIP;
        }
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
