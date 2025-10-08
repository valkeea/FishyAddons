package me.valkeea.fishyaddons.processor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.text.Text;

@SuppressWarnings("squid:S6548")
public class ChatProcessor {
    private static final ChatProcessor INSTANCE = new ChatProcessor();
    private ChatProcessor() {}
    
    public static ChatProcessor getInstance() { return INSTANCE; }
    
    private final List<ChatHandler> handlers = new CopyOnWriteArrayList<>();
    private volatile boolean handlersNeedSorting = false;
    
    // Monitoring
    private final AtomicLong messagesProcessed = new AtomicLong(0);
    private final AtomicLong totalProcessingTimeNs = new AtomicLong(0);
    
    private volatile boolean enabled = true;
    private volatile boolean debugMode = false;
    private volatile int maxProcessingTimeWarningMs = 10;

    public void registerHandler(ChatHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }
        
        String handlerName = handler.getHandlerName();
        for (ChatHandler existing : handlers) {
            if (existing.getHandlerName().equals(handlerName)) {
                throw new IllegalArgumentException("Handler with name '" + handlerName + "' already registered");
            }
        }
        
        handlers.add(handler);
        handlersNeedSorting = true;
    }

    public void onMessage(Text message, boolean overlay) {
        if (!enabled || message == null) {
            return;
        }
        
        long startTime = System.nanoTime();
        messagesProcessed.incrementAndGet();
        
        try {
            ChatMessageContext context = new ChatMessageContext(message, overlay);
            
            if (shouldSkipProcessing(context)) {
                return;
            }
            
            ensureHandlersSorted();
            processHandlers(context);
            
        } finally {
            recordProcessingTime(startTime, message);
        }
    }

    private static final AtomicReference<Text> lastPacket = new AtomicReference<>(null);

    public static void onRaw(Text packetInfo) {
        if (!INSTANCE.enabled || packetInfo == null) return;
        lastPacket.set(packetInfo);
    }

    public Text applyDisplayFilters(Text message) {
        if (!enabled || message == null) {
            return message;
        }
        
        Text currentMessage = message;
        
        try {
            Text filteredMessage = me.valkeea.fishyaddons.handler.ChatFilter.applyFilters(currentMessage);
            if (!filteredMessage.equals(currentMessage)) {
                currentMessage = filteredMessage;
            }
        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in ChatFilter: " + e.getMessage());
        }
        
        ensureHandlersSorted();

        var packetInfo = lastPacket.get() != null ? lastPacket.get() : currentMessage;
        var context = new ChatMessageContext(currentMessage, false, packetInfo);
        lastPacket.set(null);

        for (ChatHandler handler : handlers) {
            String handlerName = handler.getHandlerName();
            boolean isDisplayHandler = isDisplayOnlyHandler(handlerName);
            
            if (isDisplayHandler && handler.isEnabled() && handler.shouldHandle(context)) {
                try {
                    ChatHandlerResult result = handler.handle(context);
                    
                    if (result.hasModification()) {
                        context = result.getModifiedContext();
                        currentMessage = context.getOriginalMessage();
                    }
                } catch (Exception e) {
                    System.err.println("[FishyAddons] Error in display handler '" + 
                                     handlerName + "': " + e.getMessage());
                }
            }
        }
        
        return currentMessage;
    }
    
    private boolean isDisplayOnlyHandler(String handlerName) {
        return handlerName.equals("Coordinates") ||
                handlerName.equals("ChatButton");
    }
    
    /**
     * Process the message through all handlers
     */
    private void processHandlers(ChatMessageContext initialContext) {
        ChatMessageContext currentContext = initialContext;
        List<String> processedBy = debugMode ? new ArrayList<>() : null;

        boolean shouldStop = false;
        for (ChatHandler handler : handlers) {
            boolean isDisplayOnly = isDisplayOnlyHandler(handler.getHandlerName());
            ProcessingResult result = null;
            if (!isDisplayOnly) {
                result = processSingleHandler(handler, currentContext, processedBy);
                if (result.hasModification()) {
                    currentContext = result.getModifiedContext();
                }
                if (result.shouldStop()) {
                    shouldStop = true;
                }
            }
            if (shouldStop) {
                break;
            }
        }

        logProcessedBy(processedBy);
    }

    private ProcessingResult processSingleHandler(ChatHandler handler, ChatMessageContext context, List<String> processedBy) {
        ProcessingResult result = processHandler(handler, context);
        if (!result.wasSkipped() && debugMode && processedBy != null) {
            processedBy.add(handler.getHandlerName() + ":" + result.getAction());
        }
        return result;
    }

    private void logProcessedBy(List<String> processedBy) {
        if (debugMode && processedBy != null && !processedBy.isEmpty()) {
            System.out.println("[FishyAddons] Message processed by: " + String.join(", ", processedBy));
        }
    }
    
    private ProcessingResult processHandler(ChatHandler handler, ChatMessageContext context) {
        if (!handler.isEnabled()) {
            return ProcessingResult.skipped();
        }
        
        try {
            if (!handler.shouldHandle(context)) {
                return ProcessingResult.skipped();
            }
            
            ChatHandlerResult result = handler.handle(context);
            
            return new ProcessingResult(result.getAction(), result.getModifiedContext());
            
        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in chat handler '" + handler.getHandlerName() + "': " + e.getMessage());
            if (debugMode) {
                e.printStackTrace();
            }
            return ProcessingResult.skipped();
        }
    }
    
    /**
     * Record total processing time and warn if too slow
     */
    private void recordProcessingTime(long startTime, Text message) {
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        totalProcessingTimeNs.addAndGet(totalTime);
        
        if (totalTime > maxProcessingTimeWarningMs * 1_000_000L) {
            String messagePreview = message.getString().substring(0, Math.min(50, message.getString().length()));
            System.err.println("[FishyAddons] Chat processing took " + (totalTime / 1_000_000.0) + "ms for message: " + messagePreview);
        }
    }

    // Processing result for each chat handler
    private static class ProcessingResult {
        private final ChatHandlerResult.Action action;
        private final ChatMessageContext modifiedContext;
        private final boolean skipped;
        
        private ProcessingResult(ChatHandlerResult.Action action, ChatMessageContext modifiedContext) {
            this.action = action;
            this.modifiedContext = modifiedContext;
            this.skipped = false;
        }
        
        private ProcessingResult() {
            this.action = null;
            this.modifiedContext = null;
            this.skipped = true;
        }
        
        static ProcessingResult skipped() {
            return new ProcessingResult();
        }
        
        boolean wasSkipped() { return skipped; }
        boolean shouldStop() { return action == ChatHandlerResult.Action.STOP; }
        boolean hasModification() { return action == ChatHandlerResult.Action.MODIFY && modifiedContext != null; }
        ChatMessageContext getModifiedContext() { return modifiedContext; }
        ChatHandlerResult.Action getAction() { return action; }
    }

    private boolean shouldSkipProcessing(ChatMessageContext context) {
        return context.getCleanText().trim().isEmpty();
    }
    
    private void ensureHandlersSorted() {
        if (handlersNeedSorting) {
            synchronized (this) {
                if (handlersNeedSorting) {
                    handlers.sort(Comparator.comparingInt(ChatHandler::getPriority).reversed());
                    handlersNeedSorting = false;
                }
            }
        }
    }
    
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    
    public boolean isEnabled() { return enabled; }
    public boolean isDebugMode() { return debugMode; }
    public double getAverageProcessingTimeMs() {
        long messages = messagesProcessed.get();
        if (messages == 0) return 0.0;
        return (totalProcessingTimeNs.get() / (double) messages) / 1_000_000.0;
    }
}