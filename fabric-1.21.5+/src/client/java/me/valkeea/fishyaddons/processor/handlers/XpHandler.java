package me.valkeea.fishyaddons.processor.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.tracker.SkillTracker;

public class XpHandler implements ChatHandler {
    
    private final Map<String, String> lastSeenProgress = new HashMap<>();
    
    @Override
    public int getPriority() {
        return 55;
    }
    
    @Override
    public String getHandlerName() {
        return "XP";
    }
    
    @Override
    public boolean shouldHandle(ChatMessageContext context) {
        return context.isOverlay();
    }
    
    @Override
    public ChatHandlerResult handle(ChatMessageContext context) {
        String message = context.getUnfilteredCleanText();
        
        try {
            if (handleXp(message)) return ChatHandlerResult.STOP;
            return ChatHandlerResult.CONTINUE;
            
        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in XP handler: " + e.getMessage());
            return ChatHandlerResult.SKIP;
        }
    }
    
    private boolean handleXp(String message) {

        var xpPattern = Pattern.compile("\\+(\\d{1,3}(,\\d{3})*(\\.\\d{1,2})?) ([A-Z][a-z]+) \\(([^)]+)\\)");
        var matcher = xpPattern.matcher(message);

        if (matcher.find()) {

            var amountStr = matcher.group(1).replace(",", "");
            var skillName = matcher.group(4);
            var progressInfo = matcher.group(5);
            var lastProgress = lastSeenProgress.get(skillName);

            if (progressInfo.equals(lastProgress)) {
                return true;
            }
            
            lastSeenProgress.put(skillName, progressInfo);
            
            try {
                SkillTracker.getInstance().onXpGain(skillName, progressInfo);
                return true;

            } catch (NumberFormatException e) {
                System.err.println("[FishyAddons] Error parsing XP amount: " + amountStr);
            }
        }
        return false;
    }
    
    @Override
    public boolean isEnabled() {
        return SkillTracker.isEnabled();
    }
}