package me.valkeea.fishyaddons.processor;

public class ChatHandlerResult {
    
    public enum Action {
        CONTINUE, // Handler processed the message but other handlers should continue
        STOP, // Handler processed the message and no further handlers should run
        SKIP, // Didn't process, continue to next handler
        MODIFY // Handler wants to modify the message for subsequent handlers
    }
    
    private final Action action;
    private final String details;
    private final ChatMessageContext modifiedContext;
    private final long processingTimeNs;
    
    public static final ChatHandlerResult CONTINUE = new ChatHandlerResult(Action.CONTINUE, null, null, 0);
    public static final ChatHandlerResult STOP = new ChatHandlerResult(Action.STOP, null, null, 0);
    public static final ChatHandlerResult SKIP = new ChatHandlerResult(Action.SKIP, null, null, 0);
    
    public ChatHandlerResult(Action action, String details, ChatMessageContext modifiedContext, long processingTimeNs) {
        this.action = action;
        this.details = details;
        this.modifiedContext = modifiedContext;
        this.processingTimeNs = processingTimeNs;
    }
    
    public static ChatHandlerResult continueWith(String details) {
        return new ChatHandlerResult(Action.CONTINUE, details, null, 0);
    }
    
    public static ChatHandlerResult stopWith(String details) {
        return new ChatHandlerResult(Action.STOP, details, null, 0);
    }
    
    public static ChatHandlerResult skipWith(String details) {
        return new ChatHandlerResult(Action.SKIP, details, null, 0);
    }
    
    public static ChatHandlerResult modifyWith(ChatMessageContext newContext, String details) {
        return new ChatHandlerResult(Action.MODIFY, details, newContext, 0);
    }
    
    public static ChatHandlerResult withTiming(ChatHandlerResult result, long processingTimeNs) {
        return new ChatHandlerResult(result.action, result.details, result.modifiedContext, processingTimeNs);
    }
    
    public Action getAction() { return action; }
    public String getDetails() { return details; }
    public ChatMessageContext getModifiedContext() { return modifiedContext; }
    public long getProcessingTimeNs() { return processingTimeNs; }
    
    public boolean shouldContinue() { return action == Action.CONTINUE || action == Action.MODIFY; }
    public boolean shouldStop() { return action == Action.STOP; }
    public boolean wasProcessed() { return action != Action.SKIP; }
    public boolean hasModification() { return action == Action.MODIFY && modifiedContext != null; }
    
    @Override
    public String toString() {
        return String.format("ChatHandlerResult{action=%s, details='%s', processingTime=%dns}", 
                           action, details, processingTimeNs);
    }
}
