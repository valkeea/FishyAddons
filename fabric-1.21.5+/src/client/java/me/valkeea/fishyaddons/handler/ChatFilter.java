package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FilterConfig.MessageContext;
import me.valkeea.fishyaddons.config.FilterConfig.Rule;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.processor.MessageAnalysis;
import me.valkeea.fishyaddons.processor.MessageAnalysis.FilterMatch;
import me.valkeea.fishyaddons.processor.SharedMessageDetector;
import me.valkeea.fishyaddons.util.text.Enhancer;
import net.minecraft.text.Text;

public class ChatFilter {
    private ChatFilter() {}
    
    public static Text applyFilters(Text originalMessage) {
        String originalText = originalMessage.getString();
        String replacementText = processMessageWithContext(originalText);
        
        if (replacementText != null && !replacementText.equals(originalText)) {

            if (replacementText.isEmpty()) {
                return Text.literal("");
            }
            
            return Enhancer.parseFormattedText(replacementText);
        }
        
        return originalMessage;
    }

    public static String findContextualReplacement(String message) {
        MessageAnalysis analysis = SharedMessageDetector.analyzeMessage(message);
        
        for (FilterMatch filterMatch : analysis.getFilterMatches()) {
            Rule rule = filterMatch.getRule();
            String replacement = rule.getContextualReplacement();
            if (replacement != null) {
                return replacement;
            }
        }

        String hardcodedResult = checkHardcodedRules(message);
        if (hardcodedResult != null) {
            return hardcodedResult;
        }        
        
        return null;
    }
    
    /**
     * Records a message for contextual tracking and finds any matching replacement
     */
    public static String processMessageWithContext(String message) {

        MessageContext.recordMessage(message);
        return findContextualReplacement(message);
    }
    
    private static String checkHardcodedRules(String message) {
        if (FishyConfig.getState(Key.CHAT_FILTER_HIDE_SACK_MESSAGES, false) &&
            message.startsWith("[Sacks] +")) {
            return "";

        }
        
        if (FishyConfig.getState(Key.CHAT_FILTER_HIDE_AUTOPET_MESSAGES, false) &&
            (message.startsWith("§cAutopet §eequipped your"))) {
            return "";

        }

        if (FishyConfig.getState(Key.CHAT_FILTER_HIDE_HYPE, false) &&
            (message.startsWith("Your Implosion hit ") ||
            message.startsWith("There are blocks in the way!"))) {
            return "";
        }

        return null;
    }    
}
