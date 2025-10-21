package me.valkeea.fishyaddons.processor.handlers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.api.skyblock.GameChat;
import me.valkeea.fishyaddons.handler.CakeTimer;
import me.valkeea.fishyaddons.handler.PetInfo;
import me.valkeea.fishyaddons.handler.TransLava;
import me.valkeea.fishyaddons.listener.WorldEvent;
import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.util.AreaUtils;
import me.valkeea.fishyaddons.util.SkyblockCheck;

public class GameplayHandler implements ChatHandler {
    
    private static final Pattern CATACOMBS_PATTERN = Pattern.compile("entered (MM )?The Catacombs");
    
    @Override
    public int getPriority() {
        return 80;
    }
    
    @Override
    public String getHandlerName() {
        return "Gameplay";
    }
    
    @Override
    public boolean shouldHandle(ChatMessageContext context) {
        return !context.isOverlay();
    }
    
    @Override
    public ChatHandlerResult handle(ChatMessageContext context) {
        String message = context.getUnfilteredCleanText();
        
        try {
            if (handleGamemodeDetection(message)) return ChatHandlerResult.STOP;
            if (handleAreaChanges(message)) return ChatHandlerResult.STOP;
            if (handlePetInfo(context.getRawText())) return ChatHandlerResult.STOP;
            if (handleChatMode(message)) return ChatHandlerResult.STOP;
            if (handleBeaconFrequency(message)) return ChatHandlerResult.STOP;
            if (handleCakeTimer(context.getRawText())) return ChatHandlerResult.STOP;
            return ChatHandlerResult.CONTINUE;
            
        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in Gameplay handler: " + e.getMessage());
            return ChatHandlerResult.SKIP;
        }
    }
    
    private boolean handleAreaChanges(String message) {
        Matcher catacombsMatcher = CATACOMBS_PATTERN.matcher(message);
        if (catacombsMatcher.find()) {
            WorldEvent.getInstance().bypass();
            AreaUtils.setIsland("dungeon");
            return true;
        }
        
        if (message.contains("Glacite Mineshafts") && message.contains("entered")) {
            WorldEvent.getInstance().bypass();
            AreaUtils.setIsland("mineshaft");
            return true;
        }

        return false;
    }
    
    private boolean handleGamemodeDetection(String message) {
        if (message.contains("Welcome to Hypixel SkyBlock!")) {
            SkyblockCheck.getInstance().bypass();
            TransLava.update();
            return true;
        }
        return false;
    }

    private boolean handleBeaconFrequency(String message) {
        if (message.contains("You adjusted the frequency of the Beacon!")) {
            me.valkeea.fishyaddons.handler.ChatTimers.getInstance().beaconStart();
            return true;
        }
        return false;
    }

    private boolean handleChatMode(String message) {
        if (message.equals("You are not in a party and were moved to the ALL channel.") ||
            message.equals("You must be in a party to join the party channel!") ||
            message.equals("The party was disbanded because all invites expired and the party was empty.")) {
            GameChat.setChannel(GameChat.Channel.ALL);
            GameChat.partyStatus(false);
            return true;
        }
        if (message.equals("You have joined the party channel.") ||
            message.equals("You are already in the party channel.")) {
            GameChat.setChannel(GameChat.Channel.PARTY);
            return true;
        }
        if (message.matches("You have joined .* party!") ||
            message.matches(".* joined the party.") ||
            message.startsWith("Created a public party! Players can join with")) {
            GameChat.partyStatus(true);
            return true;
        }
        if (message.equals("You left the party.") ||
            message.equals("You are not in a party right now.") ||
            message.matches(".* disbanded the party!")) {
            GameChat.partyStatus(false);
            return true;
        }
        return false;
    }
    
    private boolean handlePetInfo(String rawMessage) {
        return PetInfo.handleChat(rawMessage);
    }

    private boolean handleCakeTimer(String rawMessage) {
        return CakeTimer.getInstance().handleChat(rawMessage);
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}