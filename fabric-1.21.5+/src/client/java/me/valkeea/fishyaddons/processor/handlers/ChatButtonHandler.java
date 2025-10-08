package me.valkeea.fishyaddons.processor.handlers;

import java.util.regex.Pattern;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.util.text.ChatButton;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class ChatButtonHandler implements ChatHandler {
    
    @Override
    public int getPriority() {
        return 40;
    }
    
    @Override
    public String getHandlerName() {
        return "ChatButton";
    }
    
    @Override
    public boolean shouldHandle(ChatMessageContext context) {
        String rawText = context.getPacketInfo();
        return context.isGuildMessage() &&
                containsLfMsg(rawText);
    }
    
    @Override
    public ChatHandlerResult handle(ChatMessageContext context) {
        try {
            Text newLine = context.getOriginalMessage();
            String rawOriginal = context.getPacketInfo();

            Text enhanced = addButton(newLine, rawOriginal);
            if (!enhanced.equals(newLine)) {
                ChatMessageContext newContext = new ChatMessageContext(enhanced, context.isOverlay());
                    return ChatHandlerResult.modifyWith(newContext, "Added chat button");
                }

            return ChatHandlerResult.CONTINUE;

        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in ChatButton handler: " + e.getMessage());
            return ChatHandlerResult.SKIP;
        }
    }
    
    @Override
    public boolean isEnabled() {
        return FishyConfig.getState(Key.CHAT_FILTER_PARTYBTN, false);
    }
    
    private boolean containsLfMsg(String message) {
        var pattern = Pattern.compile("(?i)\\b(invite|inv| p |party)\\b");
        var matcher = pattern.matcher(message);
        return matcher.find() && !message.contains("Party >");
    }

    private String extractIgn(String s) {
        var bridgePattern = Pattern.compile(
            "^Guild > (?:\\[[^\\]]+\\] )?(\\w{3,16})(?: \\[[^\\]]+\\])?: (\\w{3,16}):");
        var bridgeMatcher = bridgePattern.matcher(s);

        var ignAfterKeyword = Pattern.compile(
            "(?i)\\b(invite|inv| p |party)\\b\\s+(\\w{3,16})\\b");

        if (bridgeMatcher.find()) {
            var ignMatcher = ignAfterKeyword.matcher(s);
            if (ignMatcher.find() && !ignore(ignMatcher.group(2))) {
                return ignMatcher.group(2);
            }
            return "";
        }

        var ignMatcher = ignAfterKeyword.matcher(s);
        if (ignMatcher.find() && !ignore(ignMatcher.group(2))) {
            return ignMatcher.group(2);
        }

        var guildSender = Pattern.compile(
            "^Guild > (?:\\[[^\\]]+\\] )?(\\w{3,16})(?: \\[[^\\]]+\\])?:");
        var senderMatcher = guildSender.matcher(s);
        if (senderMatcher.find()) {
            return senderMatcher.group(1);
        }
        return "";
    }

    private boolean ignore(String addOn) {
        String lower = addOn.toLowerCase();
        return lower.equals("pls") || lower.equals("please");
    }

    private Text addButton(Text originalMessage, String cleanOriginal) {
        String ign = extractIgn(cleanOriginal);

        if (ign.isEmpty()) {
            return originalMessage;
        }
        
        MutableText hideBtn = ChatButton.create("/party " + ign, "Party");

        return originalMessage.copy().append(hideBtn);
    }
}