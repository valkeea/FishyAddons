package me.valkeea.fishyaddons.processor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.valkeea.fishyaddons.config.FilterConfig.MessageContext;
import me.valkeea.fishyaddons.event.impl.GameMessageEvent;
import me.valkeea.fishyaddons.feature.qol.ChatFilter;
import net.minecraft.text.Text;

@SuppressWarnings("squid:S6548")
public class ChatProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger("FishyAddons/ChatProcessor");
    private static final ChatProcessor INSTANCE = new ChatProcessor();
    public static ChatProcessor getInstance() { return INSTANCE; }
    private ChatProcessor() {}

    private final List<ChatHandler> handlers = new CopyOnWriteArrayList<>();
    private final ThreadLocal<ChatMessageContext> pendingDisplayContext = ThreadLocal.withInitial(() -> null);

    // Monitoring
    private final AtomicLong messagesProcessed = new AtomicLong(0);
    private final AtomicLong totalProcessingTimeNs = new AtomicLong(0);
    
    private volatile boolean debugMode = false;
    private volatile boolean handlersNeedSorting = false;    
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

    public void onMessage(GameMessageEvent event) {
        if (event.message == null) {
            return;
        }

        long startTime = System.nanoTime();
        messagesProcessed.incrementAndGet();
        
        try {

            var context = new ChatMessageContext(event.message, event.overlay);
            ChatEvents.dispatch(context);
            MessageContext.recordMessage(context.getRawString());

            if (!event.overlay) pendingDisplayContext.set(context);
            
            if (shouldSkipProcessing(context)) return;

            ensureHandlersSorted();
            processInternalHandlers(context);
            
        } finally {
            recordProcessingTime(startTime, event.message);
        }
    }

    public Text applyDisplayFilters(Text message) {
        if (message == null) return message;

        var context = pendingDisplayContext.get();
        if (context == null) return message;

        pendingDisplayContext.remove();
        
        if (!message.equals(context.getOriginalText())) {
            context = new ChatMessageContext(context.getOriginalText(), message, context.isOverlay());
        }

        try {
            Text filteredMessage = ChatFilter.applyFilters(context);
            context.setCurrentMessage(filteredMessage);

            processDisplayHandlers(context);
            
            return context.getCurrentMessage();
            
        } catch (Exception e) {
            LOGGER.error("Error in display filters: {}", e.getMessage());
            return message;
        }
    }

    private void processInternalHandlers(ChatMessageContext context) {
        List<String> processedBy = debugMode ? new ArrayList<>() : null;
        var currentContext = context;

        for (ChatHandler handler : handlers) {
            if (!isDisplayOnlyHandler(handler)) {
                var result = processSingleHandler(handler, currentContext, processedBy);
                if (result.shouldStop()) {
                    break;
                }
            }
        }

        logProcessedBy(processedBy);
    }

    private void processDisplayHandlers(ChatMessageContext context) {
        List<String> processedBy = debugMode ? new ArrayList<>() : null;

        for (var handler : handlers) {
            if (isDisplayOnlyHandler(handler) && handler.isEnabled()) {
                processDisplayHandler(handler, context, processedBy);
            }
        }

        logProcessedBy(processedBy);
    }

    private void processDisplayHandler(ChatHandler handler, ChatMessageContext context, List<String> processedBy) {

        try {
            if (handler.shouldHandle(context)) {
                ChatHandlerResult result = handler.handle(context);

                if (debugMode && processedBy != null) {
                    processedBy.add(handler.getHandlerName() + ":" + result.getAction());
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error in display handler '{}': {}", handler.getHandlerName(), e.getMessage());
        }
    }

    private boolean isDisplayOnlyHandler(ChatHandler handler) {
        return handler.isDisplay();
    }
    
    private ProcessingResult processSingleHandler(ChatHandler handler, ChatMessageContext context, List<String> processedBy) {
        ProcessingResult result = processHandler(handler, context);
        if (!result.wasSkipped() && debugMode && processedBy != null) {
            processedBy.add(handler.getHandlerName() + ":" + result.getAction());
        }
        return result;
    }

    private void logProcessedBy(List<String> processedBy) {
        if (debugMode && processedBy != null && !processedBy.isEmpty() && LOGGER.isInfoEnabled()) {
            LOGGER.info("Message processed by: {}", String.join(", ", processedBy));
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
            return new ProcessingResult(result.getAction());

        } catch (Exception e) {
            LOGGER.error("Error in chat handler '{}': {}", handler.getHandlerName(), e.getMessage());
            if (debugMode) {
                e.printStackTrace();
            }
            return ProcessingResult.skipped();
        }
    }

    private void recordProcessingTime(long startTime, Text message) {
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        totalProcessingTimeNs.addAndGet(totalTime);
        
        if (totalTime > maxProcessingTimeWarningMs * 1_000_000L) {
            String messagePreview = message.getString().substring(0, Math.min(50, message.getString().length()));
            LOGGER.warn("Chat processing took {}ms for message: {}", (totalTime / 1_000_000.0), messagePreview);
        }
    }

    private static class ProcessingResult {
        private final ChatHandlerResult.Action action;
        private final boolean skipped;
        
        private ProcessingResult(ChatHandlerResult.Action action) {
            this.action = action;
            this.skipped = false;
        }
        
        private ProcessingResult() {
            this.action = null;
            this.skipped = true;
        }
        
        static ProcessingResult skipped() {
            return new ProcessingResult();
        }
        
        boolean wasSkipped() { return skipped; }
        boolean shouldStop() { return action == ChatHandlerResult.Action.STOP; }
        ChatHandlerResult.Action getAction() { return action; }
    }

    private boolean shouldSkipProcessing(ChatMessageContext context) {
        return context.getCleanString().trim().isEmpty();
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
    
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    public boolean isDebugMode() { return debugMode; }
    public double getAverageProcessingTimeMs() {
        long messages = messagesProcessed.get();
        if (messages == 0) return 0.0;
        return (totalProcessingTimeNs.get() / (double) messages) / 1_000_000.0;
    }
}
