package me.valkeea.fishyaddons.feature.filter;

import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.processor.MessageAnalysis.FilterMatch;
import me.valkeea.fishyaddons.util.text.Enhancer;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
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
        return !Config.get(BooleanKey.CHAT_FILTER_SC_ENABLED) &&
                (context.isSeaCreatureMessage() || firstMatch.getRuleName().startsWith("dh_trigger"));
    }
    
    private static String checkHardcodedRules(String message) {
        if (Config.get(BooleanKey.CHAT_FILTER_HIDE_SACK_MESSAGES) &&
            message.startsWith("[Sacks] +")) {
            return "";

        }
        
        if (Config.get(BooleanKey.CHAT_FILTER_HIDE_AUTOPET_MESSAGES) &&
            (message.startsWith("§cAutopet §eequipped your"))) {
            return "";

        }

        if (Config.get(BooleanKey.CHAT_FILTER_HIDE_HYPE) &&
            (message.startsWith("Your Implosion hit ") ||
            message.startsWith("There are blocks in the way!"))) {
            return "";
        }

        return null;
    }    
}
