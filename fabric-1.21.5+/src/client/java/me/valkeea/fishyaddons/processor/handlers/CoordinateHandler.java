package me.valkeea.fishyaddons.processor.handlers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.handler.ActiveBeacons;
import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.HelpUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class CoordinateHandler implements ChatHandler {
    
    private static final Pattern COORD_PATTERN = Pattern.compile(
        "\\bx\\s*:\\s*(-?\\d{1,7})\\s*,\\s*y\\s*:\\s*(-?\\d{1,7})\\s*,\\s*z\\s*:\\s*(-?\\d{1,7})\\b",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public int getPriority() {
        return 45;
    }
    
    @Override
    public String getHandlerName() {
        return "Coordinates";
    }
    
    @Override
    public boolean shouldHandle(ChatMessageContext context) {
        String rawText = context.getRawText();
        return !context.isOverlay() && 
            (FishyConfig.getState(Key.RENDER_COORDS, true) || 
                FishyConfig.getState(Key.CHAT_FILTER_COORDS_ENABLED, true)) && 
            containsCoordinates(rawText) &&
            !rawText.contains("[Hide]") &&
            !rawText.contains("[Redraw]");
    }
    
    @Override
    public ChatHandlerResult handle(ChatMessageContext context) {
        try {
            String rawMessage = context.getRawText();
            Text originalMessage = context.getOriginalMessage();
            
            if (FishyConfig.getState(Key.RENDER_COORDS, true)) {
                initBeaconFor(rawMessage);
                if (FishyConfig.getState(Key.CHAT_FILTER_COORDS_ENABLED, true)) {
                    Text enhanced = addButtons(originalMessage);
                    if (!enhanced.equals(originalMessage)) {
                        ChatMessageContext newContext = new ChatMessageContext(enhanced, context.isOverlay());
                        return ChatHandlerResult.modifyWith(newContext, "Added coordinate enhancement");
                    }
                }                
            }
            
            return ChatHandlerResult.CONTINUE;
        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in Coordinate handler: " + e.getMessage());
            return ChatHandlerResult.SKIP;
        }
    }
    
    @Override
    public boolean isEnabled() {
        return FishyConfig.getState(Key.RENDER_COORDS, true) || 
               FishyConfig.getState(Key.CHAT_FILTER_COORDS_ENABLED, true);
    }
    
    private void initBeaconFor(String rawMessage) {
        Matcher matcher = COORD_PATTERN.matcher(rawMessage);
        if (!matcher.find()) {
            return;
        }
        
        try {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            BlockPos newPos = new BlockPos(x, y, z);
            
            String label = "";
            int endOfCoords = matcher.end();
            if (endOfCoords < rawMessage.length()) {
                label = HelpUtil.stripColor(rawMessage.substring(endOfCoords).trim());
            }
            
            ActiveBeacons.setBeacon(
                newPos,
                FishyConfig.getInt(Key.RENDER_COORD_COLOR, -5653771), 
                label
            );
        } catch (NumberFormatException e) {
            // Invalid number format, ignore
        }
    }
    
    private boolean containsCoordinates(String message) {
        return message.toLowerCase().contains("x:") && 
               message.toLowerCase().contains("y:") && 
               message.toLowerCase().contains("z:");
    }
    
    private String extractCoordinates(String message) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("x:\\s*(-?\\d+),?\\s*y:\\s*(-?\\d+),?\\s*z:\\s*(-?\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        
        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2) + " " + matcher.group(3);
        }
        return "";
    }

    private String extractTitle(String message) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("x:\\s*(-?\\d+),?\\s*y:\\s*(-?\\d+),?\\s*z:\\s*(-?\\d+)(.*)");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        
        if (matcher.find() && matcher.groupCount() >= 4) {
            return matcher.group(4).trim();
        }
        return "";
    }

    private Text addButtons(Text originalMessage) {
        String messageText = originalMessage.getString();
        String coords = extractCoordinates(messageText);
        String title = extractTitle(messageText);
        
        MutableText hideButton = Text.literal(" [")
            .styled(style -> style.withColor(0xFF808080))
            .append((Text.literal("Hide"))
            .styled(style -> style.withClickEvent(
                new net.minecraft.text.ClickEvent.RunCommand("/fa coords hide " + coords)
            ).withColor(FishyMode.getCmdColor())))
            .append(Text.literal("]").styled(style -> style.withColor(0xFF808080)));

        MutableText reDrawButton = Text.literal(" [")
            .styled(style -> style.withColor(0xFF808080))
            .append((Text.literal("Redraw"))
            .styled(style -> style.withClickEvent(
                new net.minecraft.text.ClickEvent.RunCommand("/fa coords redraw " + coords + (title.isEmpty() ? "" : " " + title))
            ).withColor(FishyMode.getCmdColor())))
            .append(Text.literal("]").styled(style -> style.withColor(0xFF808080)));

        return originalMessage.copy().append(hideButton).append(reDrawButton);
    }
}
