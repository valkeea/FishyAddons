package me.valkeea.fishyaddons.processor.handlers;

import java.util.regex.Pattern;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import me.valkeea.fishyaddons.api.skyblock.GameChat;
import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.feature.skyblock.timer.CakeTimer;
import me.valkeea.fishyaddons.feature.skyblock.PetInfo;
import me.valkeea.fishyaddons.listener.WorldEvent;
import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;

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
        String message = context.getCleanString();
        try {
            if (handleGamemodeDetection(message)) return ChatHandlerResult.STOP;
            if (handleAreaChanges(message)) return ChatHandlerResult.STOP;
            if (handlePetInfo(context.getRawString())) return ChatHandlerResult.STOP;
            if (handleChatMode(message)) return ChatHandlerResult.STOP;
            if (handleBeaconFrequency(message)) return ChatHandlerResult.STOP;
            if (handleCakeTimer(context.getRawString())) return ChatHandlerResult.STOP;
            if (handleVial(context.getLowerCleanString())) return ChatHandlerResult.STOP;
            if (handleWaypointChains(message)) return ChatHandlerResult.CONTINUE;
            return ChatHandlerResult.CONTINUE;
            
        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in Gameplay handler: " + e.getMessage());
            return ChatHandlerResult.SKIP;
        }
    }
    
    private boolean handleAreaChanges(String message) {
        var catacombsMatcher = CATACOMBS_PATTERN.matcher(message);
        if (catacombsMatcher.find()) {
            WorldEvent.getInstance().bypass();
            SkyblockAreas.setIsland(Island.DUNGEON);
            return true;
        }
        
        if (message.contains("Glacite Mineshafts") && message.contains("entered")) {
            WorldEvent.getInstance().bypass();
            SkyblockAreas.setIsland(Island.MINESHAFT);
            return true;
        }

        return false;
    }

    private boolean handleGamemodeDetection(String message) {
        if (handleApiMessages(message)) return true;

        if (message.contains("Welcome to Hypixel SkyBlock!")) {
            GameMode.confirm();
            return true;
        }
        return false;
    }

    private boolean handleBeaconFrequency(String message) {
        if (message.contains("You adjusted the frequency of the Beacon!")) {
            me.valkeea.fishyaddons.feature.skyblock.timer.ChatTimers.getInstance().beaconStart();
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
    
    private boolean handleWaypointChains(String message) {
        if (message.contains("Relics)") || message.contains("You've already found this relic!") ||
            message.contains("You've already found all the relics!")) {

            me.valkeea.fishyaddons.feature.waypoints.WaypointChains.onRelicFound();
            return true;
        }

        return false;
    }

    private boolean handleVial(String message) {
        return me.valkeea.fishyaddons.tracker.fishing.ScStats.getInstance().checkForVial(message);
    }

    private boolean handleApiMessages(String message) {
        if (message.startsWith("{\"server\":")) {
            
            try {
                var jsonObject = JsonParser.parseString(message).getAsJsonObject();
                String typeKey = "gametype";
                
                String gametype = null;
                if (jsonObject.has(typeKey) && !jsonObject.get(typeKey).isJsonNull()) {
                    gametype = jsonObject.get(typeKey).getAsString();
                }
                
                String map = null;
                if (jsonObject.has("map") && !jsonObject.get("map").isJsonNull()) {
                    map = jsonObject.get("map").getAsString();
                }
                
                if (gametype != null && gametype.equals("SKYBLOCK")) {
                    GameMode.confirm();
                    if (map != null) {
                        SkyblockAreas.setIslandByMap(map);
                    }
                    return true;
                }
                
            } catch (JsonSyntaxException | IllegalStateException e) {
                System.err.println("[FishyAddons] Failed to parse API message: " + e.getMessage());
            }
        }
        return false;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
