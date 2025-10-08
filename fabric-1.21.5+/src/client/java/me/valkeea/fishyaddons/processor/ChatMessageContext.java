package me.valkeea.fishyaddons.processor;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.text.Text;

/**
 * Context object to encapsulate chat message data for handlers
 */
public class ChatMessageContext {
    private final Text originalMessage;
    private final Text unfilteredMessage;
    private final String rawText;
    private final String cleanText;
    private final String packetInfo;
    private final String lowercaseText;
    private final boolean overlay;
    private final long timestamp;
    
    private Boolean isSkyblockMessage;
    private Boolean isSystemMessage;
    private Boolean isPlayerMessage;
    private Boolean isGuildMessage;

    public ChatMessageContext(Text originalMessage, boolean overlay, @Nullable Text packetInfo) {
        this(originalMessage, originalMessage, overlay, packetInfo);
    }

    public ChatMessageContext(Text originalMessage, boolean overlay) {
        this(originalMessage, originalMessage, overlay, null);
    }    
    
    public ChatMessageContext(Text originalMessage, Text unfilteredMessage, boolean overlay, @Nullable Text packetInfo) {
        this.originalMessage = originalMessage;
        this.unfilteredMessage = unfilteredMessage;
        this.overlay = overlay;
        this.timestamp = System.currentTimeMillis();
        this.rawText = originalMessage.getString();
        this.cleanText = TextUtils.stripColor(rawText);
        this.packetInfo = packetInfo != null ? TextUtils.stripColor(packetInfo.getString()) : "";
        this.lowercaseText = cleanText.toLowerCase();
    }
    
    public Text getOriginalMessage() { return originalMessage; }
    public Text getUnfilteredMessage() { return unfilteredMessage; }
    public String getUnfilteredText() { return unfilteredMessage.getString(); }
    public String getUnfilteredCleanText() { return TextUtils.stripColor(unfilteredMessage.getString()); }
    public String getUnfilteredCleanLowercaseText() { return TextUtils.stripColor(unfilteredMessage.getString()).toLowerCase(); }
    public String getRawText() { return rawText; }
    public String getCleanText() { return cleanText; }
    public String getPacketInfo() { return packetInfo; }
    public String getLowercaseText() { return lowercaseText; }
    public boolean isOverlay() { return overlay; }
    public long getTimestamp() { return timestamp; }
    
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
        return !isOverlay() && startsWith("[", "Guild", "Party");
    }

    public boolean guildMessage() {
        return !isOverlay() && packetInfo.startsWith("Guild >");
    }
    
    private boolean skyblockMessage() {
        return !isOverlay() && me.valkeea.fishyaddons.util.SkyblockCheck.getInstance().rules();
    }

    private boolean systemMessage() {
        return !isOverlay() && !playerMessage();
    }
    
    @Override
    public String toString() {
        return String.format("ChatMessageContext{cleanText='%s', timestamp=%d}", 
                           cleanText.substring(0, Math.min(50, cleanText.length())), timestamp);
    }
}
