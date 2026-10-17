package me.valkeea.fishyaddons.processor.handlers;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import me.valkeea.fishyaddons.api.skyblock.GameChat;
import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.feature.skyblock.PetInfo;
import me.valkeea.fishyaddons.feature.skyblock.timer.CakeTimer;
import me.valkeea.fishyaddons.listener.WorldEvent;
import me.valkeea.fishyaddons.processor.ChatHandler;
import me.valkeea.fishyaddons.processor.ChatHandlerResult;
import me.valkeea.fishyaddons.processor.ChatMessageContext;
import me.valkeea.fishyaddons.util.ZoneUtils;

public class GameplayHandler implements ChatHandler {
    
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
        String msg = context.getCleanString();
        String lower = context.getLowerCleanString();
        try {
            if (handleGamemodeDetection(msg)) return ChatHandlerResult.STOP;
            if (handleAreaChanges(msg)) return ChatHandlerResult.STOP;
            if (handlePetInfo(context.getRawString())) return ChatHandlerResult.STOP;
            if (handleChatMode(msg)) return ChatHandlerResult.STOP;
            if (handleDropCorrections(lower)) return ChatHandlerResult.STOP;
            if (handleBeaconFrequency(msg)) return ChatHandlerResult.STOP;
            if (handleCakeTimer(context.getRawString())) return ChatHandlerResult.STOP;
            if (handleVial(lower)) return ChatHandlerResult.STOP;
            if (handleWaypointChains(msg)) return ChatHandlerResult.STOP;
            return ChatHandlerResult.CONTINUE;
            
        } catch (Exception e) {
            System.err.println("[FishyAddons] Error in Gameplay handler: " + e.getMessage());
            return ChatHandlerResult.SKIP;
        }
    }
    
    private boolean handleAreaChanges(String msg) {
        if (msg.startsWith("[NPC] Mort: Here, I found this map when I first entered the dungeon.")) {
            WorldEvent.getInstance().bypass();
            ZoneUtils.startedDungeon();
            return true;
        }
        
        if (msg.contains("Glacite Mineshafts") && msg.contains("entered")) {
            WorldEvent.getInstance().bypass();
            SkyblockAreas.setIsland(Island.MINESHAFT);
            return true;
        }

        return false;
    }

    private boolean handleGamemodeDetection(String msg) {
        if (handleApiMessages(msg)) return true;

        if (msg.contains("Welcome to Hypixel SkyBlock!")) {
            GameMode.confirm();
            return true;
        }

        if (msg.equals("You were spawned in Limbo.")) {
            GameMode.leftSkyblock();
            return true;
        }
        return false;
    }

    private boolean handleDropCorrections(String s) {
        return me.valkeea.fishyaddons.tracker.profit.SackDropParser.notGain(s);
    }

    private boolean handleBeaconFrequency(String msg) {
        if (msg.contains("You adjusted the frequency of the Beacon!")) {
            me.valkeea.fishyaddons.feature.skyblock.timer.ChatTimers.getInstance().beaconStart();
            return true;
        }
        return false;
    }

    private boolean handleChatMode(String msg) {
        if (msg.equals("You are not in a party and were moved to the ALL channel.") ||
            msg.equals("You must be in a party to join the party channel!") ||
            msg.equals("The party was disbanded because all invites expired and the party was empty.")) {
            GameChat.setChannel(GameChat.Channel.ALL);
            GameChat.partyStatus(false);
            return true;
        }
        if (msg.equals("You have joined the party channel.") ||
            msg.equals("You are already in the party channel.")) {
            GameChat.setChannel(GameChat.Channel.PARTY);
            return true;
        }
        if (msg.matches("You have joined .* party!") ||
            msg.matches(".* joined the party.") ||
            msg.startsWith("Created a public party! Players can join with")) {
            GameChat.partyStatus(true);
            return true;
        }
        if (msg.equals("You left the party.") ||
            msg.equals("You are not in a party right now.") ||
            msg.matches(".* disbanded the party!")) {
            GameChat.partyStatus(false);
            return true;
        }
        return false;
    }
    
    private boolean handlePetInfo(String raw) {
        return PetInfo.handleChat(raw);
    }

    private boolean handleCakeTimer(String raw) {
        return CakeTimer.getInstance().handleChat(raw);
    }
    
    private boolean handleWaypointChains(String msg) {
        if (msg.contains("Relics)") || msg.contains("You've already found this relic!") ||
            msg.contains("You've already found all the relics!")) {

            me.valkeea.fishyaddons.feature.waypoints.WaypointChains.onRelicFound();
            return true;
        }

        return false;
    }

    private boolean handleVial(String s) {
        return me.valkeea.fishyaddons.tracker.fishing.ScStats.getInstance().checkForVial(s);
    }

    private boolean handleApiMessages(String msg) {
        if (msg.startsWith("{\"server\":")) {
            
            try {
                var jsonObject = JsonParser.parseString(msg).getAsJsonObject();
                String typeKey = "gametype";
                
                String gametype = null;
                if (jsonObject.has(typeKey) && !jsonObject.get(typeKey).isJsonNull()) {
                    gametype = jsonObject.get(typeKey).getAsString();
                }
                
                String map = null;
                if (jsonObject.has("map") && !jsonObject.get("map").isJsonNull()) {
                    map = jsonObject.get("map").getAsString();
                }
                
                return checkGameType(gametype, map);
                
            } catch (JsonSyntaxException | IllegalStateException e) {
                System.err.println("[FishyAddons] Failed to parse API message: " + e.getMessage());
            }
        }
        return false;
    }

    private boolean checkGameType(String type, String map) {
        if (type == null) return false;

        if (type.equals("SKYBLOCK")) {
            GameMode.confirm();
            if (map != null) SkyblockAreas.setIslandByMap(map);

        } else GameMode.leftSkyblock();

        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}
