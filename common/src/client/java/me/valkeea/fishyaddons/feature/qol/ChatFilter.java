package me.valkeea.fishyaddons.feature.qol;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.processor.MessageAnalysis.FilterMatch;
import me.valkeea.fishyaddons.util.text.Enhancer;
import net.minecraft.text.Text;

public class ChatFilter {
    private ChatFilter() {}
    
    public static Text applyFilters(ChatMessageContext context) {

        Text currentMsg = context.getCurrentMessage();
        String replacement = findContextualReplacement(context);

        if (replacement != null && !replacement.equals(currentMsg.getString())) {

            if (replacement.isEmpty()) {
                return Text.literal("");
            }

            return Enhancer.parseFormattedText(replacement);
        }
        
        return currentMsg;
    }

    public static String findContextualReplacement(ChatMessageContext context) {

        var firstMatch = context.getAnalysisResult().getFirstFilterMatch();
        if (firstMatch != null) {

            if (scMatchWithFilterOff(context, firstMatch)) {
                return null;
            }

            var replacement = firstMatch.getRule().getContextualReplacement(context.isDoubleHook());
            if (replacement != null) {
                return replacement;
            }
        }

        return checkHardcodedRules(context.getRawString());
    }

    private static boolean scMatchWithFilterOff(ChatMessageContext context, FilterMatch firstMatch) {
        return !FishyConfig.getState(Key.CHAT_FILTER_SC_ENABLED, false) &&
                (context.isSeaCreatureMessage() || firstMatch.getRuleName().startsWith("dh_trigger"));
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
