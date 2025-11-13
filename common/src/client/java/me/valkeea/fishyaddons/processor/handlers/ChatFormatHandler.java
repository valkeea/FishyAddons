package me.valkeea.fishyaddons.processor.handlers;

import java.util.regex.Pattern;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.util.text.ChatButton;
import me.valkeea.fishyaddons.util.text.Enhancer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class ChatFormatHandler implements ChatHandler {
    
    @Override
    public int getPriority() {
        return 40;
    }
    
    @Override
    public String getHandlerName() {
        return "ChatFormat";
    }
    
    @Override
    public boolean shouldHandle(ChatMessageContext context) {
        String rawText = context.getRawString();
        return context.isGuildMessage() &&
                containsIdentifiers(rawText);
    }
    
    @Override
    public ChatHandlerResult handle(ChatMessageContext context) {

        try {
            Text newLine = context.getCurrentMessage();
            Text original = context.getOriginalText();
            String cleanOriginal = context.getRawString();
            Text enhanced = addFormatting(newLine, original, cleanOriginal);

            if (!enhanced.equals(newLine)) {
                context.setCurrentMessage(enhanced);
            }

            return ChatHandlerResult.CONTINUE;

        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in ChatButton handler: " + e.getMessage());
            return ChatHandlerResult.SKIP;
        }
    }

    @Override
    public boolean isDisplay() {
        return true;
    }        
    
    @Override
    public boolean isEnabled() {
        return FishyConfig.getState(Key.CHAT_FILTER_PARTYBTN, false) || 
               FishyConfig.getState(Key.CHAT_FORMATTING, true);
    }
    
    private boolean containsIdentifiers(String message) {
        return hasLfgTag(message) || hasFormattingCodes(message);
    }

    private boolean hasFormattingCodes(String s) {
        return s.contains("&{") || s.contains("&[");
    }

    private boolean hasLfgTag(String s) {
        var pattern = Pattern.compile("(?i)\\b(invite|inv| p |party)\\b");
        var matcher = pattern.matcher(s);
        return (matcher.find() && !s.contains("Party >"));
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

    private Text addFormatting(Text originalMessage, Text originalText, String cleanOriginal) {
        if (hasFormattingCodes(cleanOriginal) && FishyConfig.getState(Key.CHAT_FORMATTING, true)) {
            originalMessage = Enhancer.parseExistingStyle(originalText);
        }

        if (!FishyConfig.getState(Key.CHAT_FILTER_PARTYBTN, false) || !hasLfgTag(cleanOriginal)) {
            return originalMessage;
        }

        String ign = extractIgn(cleanOriginal);

        if (ign.isEmpty()) {
            return originalMessage;
        }
        
        var hideBtn = ChatButton.create("/party " + ign, "Party");

        return originalMessage.copy().append(hideBtn);
    }
}
