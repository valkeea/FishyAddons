package me.valkeea.fishyaddons.processor;

import net.minecraft.text.Text;

/**
 * Context object to encapsulate chat message data for handlers
 */
public class ChatMessageContext {
    private final Text originalMessage;
    private final Text unfilteredMessage;
    private final String rawText;
    private final String cleanText;
    private final String lowercaseText;
    private final boolean overlay;
    private final long timestamp;
    
    private Boolean isSkyblockMessage;
    private Boolean isSystemMessage;
    private Boolean isPlayerMessage;
    
    public ChatMessageContext(Text originalMessage, boolean overlay) {
        this(originalMessage, originalMessage, overlay);
    }
    
    public ChatMessageContext(Text originalMessage, Text unfilteredMessage, boolean overlay) {
        this.originalMessage = originalMessage;
        this.unfilteredMessage = unfilteredMessage;
        this.overlay = overlay;
        this.timestamp = System.currentTimeMillis();
        this.rawText = originalMessage.getString();
        this.cleanText = me.valkeea.fishyaddons.util.HelpUtil.stripColor(rawText);
        this.lowercaseText = cleanText.toLowerCase();
    }
    
    public Text getOriginalMessage() { return originalMessage; }
    public Text getUnfilteredMessage() { return unfilteredMessage; }
    public String getUnfilteredText() { return unfilteredMessage.getString(); }
    public String getUnfilteredCleanText() { return me.valkeea.fishyaddons.util.HelpUtil.stripColor(unfilteredMessage.getString()); }
    public String getRawText() { return rawText; }
    public String getCleanText() { return cleanText; }
    public String getLowercaseText() { return lowercaseText; }
    public boolean isOverlay() { return overlay; }
    public long getTimestamp() { return timestamp; }
    
    public boolean isSkyblockMessage() {
        if (isSkyblockMessage == null) {
            isSkyblockMessage = computeIsSkyblockMessage();
        }
        return isSkyblockMessage;
    }
    
    public boolean isSystemMessage() {
        if (isSystemMessage == null) {
            isSystemMessage = computeIsSystemMessage();
        }
        return isSystemMessage;
    }
    
    public boolean isPlayerMessage() {
        if (isPlayerMessage == null) {
            isPlayerMessage = computeIsPlayerMessage();
        }
        return isPlayerMessage;
    }
    
    public boolean contains(String... keywords) {
        for (String keyword : keywords) {
            if (lowercaseText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    public boolean startsWith(String... prefixes) {
        for (String prefix : prefixes) {
            if (cleanText.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean playerMessage() {
        return startsWith("[", "Guild", "Party");
    }

    public boolean statusBar() {
        return contains("❤", "❈", "✎");
    }
    
    private boolean computeIsSkyblockMessage() {
        if (!me.valkeea.fishyaddons.util.SkyblockCheck.getInstance().rules()) {
            return false;
        }

        return !statusBar();
    }
    
    private boolean computeIsSystemMessage() {
        return !playerMessage() && !statusBar();
    }
    
    private boolean computeIsPlayerMessage() {
        return playerMessage() && !statusBar();
    }
    
    @Override
    public String toString() {
        return String.format("ChatMessageContext{cleanText='%s', timestamp=%d}", 
                           cleanText.substring(0, Math.min(50, cleanText.length())), timestamp);
    }
}
