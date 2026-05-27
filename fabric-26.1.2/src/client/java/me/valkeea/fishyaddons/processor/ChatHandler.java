package me.valkeea.fishyaddons.processor;

public interface ChatHandler {
    
    /**
     * Returns the priority of the handler (higher = processed first)
     */
    int getPriority();
    
    /**
     * Returns the name/ID of the handler
     */
    String getHandlerName();
    
    /**
     * Quick check if the handler should process the given message
     */
    boolean shouldHandle(ChatMessageContext context);
    
    /**
     * Process the chat message
     * @param context Pre-processed message context
     * @return Result indicating what happened and whether to continue processing
     */
    ChatHandlerResult handle(ChatMessageContext context);

    /**
     * If true, the handler is processed from ChatHud$addMessage
     */
    default boolean isDisplay() { return false; }

    /**
     * Whether the handler is currently enabled
     */
    default boolean isEnabled() { return true; }
}
