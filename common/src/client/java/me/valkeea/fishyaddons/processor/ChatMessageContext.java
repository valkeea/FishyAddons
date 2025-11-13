package me.valkeea.fishyaddons.processor;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.text.Text;

public class ChatMessageContext {
    private final Text originalMessage;
    private final String rawText;
    private final String cleanText;
    private final String lowerCleanText;
    private final boolean overlay;
    private final long timestamp;
    
    // Analysis results from the coordinator
    private final AnalysisCoordinator.AnalysisResult analysisResult;
    
    // Processing state
    private Text modifiedMessage;
    
    // Cached message type checks
    private Boolean isSkyblockMessage;
    private Boolean isSystemMessage;
    private Boolean isPlayerMessage;
    private Boolean isGuildMessage;

    /**
     * Initial context for a new chat message
     */
    public ChatMessageContext(Text originalMessage, boolean overlay) {
        this.originalMessage = originalMessage;
        this.overlay = overlay;
        this.timestamp = System.currentTimeMillis();
        this.rawText = originalMessage.getString();
        this.cleanText = TextUtils.stripColor(rawText);
        this.lowerCleanText = cleanText.toLowerCase();
        this.analysisResult = AnalysisCoordinator.getInstance().analyzeMessage(rawText);
        
        this.modifiedMessage = originalMessage;
    }
    
    /**
     * New context for the actual rendered message
     */
    public ChatMessageContext(Text originalMessage, Text modifiedMessage, boolean overlay) {
        this.originalMessage = originalMessage;
        this.overlay = overlay;
        this.timestamp = System.currentTimeMillis();
        this.rawText = originalMessage.getString();
        this.cleanText = TextUtils.stripColor(rawText);
        this.lowerCleanText = cleanText.toLowerCase();
        
        this.analysisResult = AnalysisCoordinator.getInstance()
            .analyzeMessage(rawText, modifiedMessage);
        
        this.modifiedMessage = modifiedMessage;
    }
    
    // --- Core Message Access ---
    
    /** Original Text object */
    public Text getOriginalText() { return originalMessage; }
    
    /** Original message as String */
    public String getRawString() { return rawText; }
    
    /** Cleaned message String */
    public String getCleanString() { return cleanText; }
    
    /** Cleaned original message String in lowercase */
    public String getLowerCleanString() { return lowerCleanText; }
    
    /** Current message state (original or modified) */
    public Text getCurrentMessage() { return modifiedMessage == null ? originalMessage : modifiedMessage; }
    
    /** Check if this is an overlay message */
    public boolean isOverlay() { return overlay; }
    
    /** Get the timestamp when the message was created */
    public long getTimestamp() { return timestamp; }
    
    // --- Analysis Results Access ---
    
    /** Complete analysis result */
    public AnalysisCoordinator.AnalysisResult getAnalysisResult() { return analysisResult; }
    
    /** Check if this message matches any sea creature patterns */
    public boolean isSeaCreatureMessage() { return analysisResult.isSeaCreatureMessage(); }
    
    /** Get the sea creature ID if this is a sea creature message */
    public @Nullable String getSeaCreatureId() { return analysisResult.getSeaCreatureId(); }
    
    /** Check if this sea creature catch was a double hook */
    public boolean isDoubleHook() { return analysisResult.isDoubleHook(); }
    
    /** Check if this message has any filter matches */
    public boolean hasFilterMatches() { return analysisResult.hasFilterMatches(); }
    
    /** Check if this message has any alert matches */
    public boolean hasAlertMatches() { return analysisResult.hasAlertMatches(); }
    
    // --- Processing State Management ---
    
    /** Update the current message state */
    public void setCurrentMessage(Text message) {
        this.modifiedMessage = message;
    }
    
    // --- Message Type Checks ---
    
    public boolean isSkyblockMessage() {
        if (isSkyblockMessage == null) {
            isSkyblockMessage = skyblockMessage();
        }
        return isSkyblockMessage;
    }
    
    public boolean isSystemMessage() {
        if (isSystemMessage == null) {
            isSystemMessage = systemMessage();
        }
        return isSystemMessage;
    }
    
    public boolean isPlayerMessage() {
        if (isPlayerMessage == null) {
            isPlayerMessage = playerMessage();
        }
        return isPlayerMessage;
    }
    
    public boolean isGuildMessage() {
        if (isGuildMessage == null) {
            isGuildMessage = guildMessage();
        }
        return isGuildMessage;
    }
    
    // --- Message Type Detection ---

    public boolean contains(String... keywords) {
        for (String keyword : keywords) {
            if (lowerCleanText.contains(keyword.toLowerCase())) {
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
    
    private boolean skyblockMessage() {
        return !isOverlay() && me.valkeea.fishyaddons.api.skyblock.GameMode.skyblock();
    }
    
    private boolean systemMessage() {
        return !isOverlay() && !playerMessage();
    }
    
    private boolean playerMessage() {
        return !isOverlay() && startsWith("[", "Guild", "Party");
    }
    
    private boolean guildMessage() {
        return !isOverlay() && getCleanString().startsWith("Guild >");
    }
}
