package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.handler.CakeTimer;
import me.valkeea.fishyaddons.handler.PetInfo;
import me.valkeea.fishyaddons.handler.WeatherTracker;
import me.valkeea.fishyaddons.render.BeaconRenderer;
import me.valkeea.fishyaddons.util.AreaUtils;
import me.valkeea.fishyaddons.util.SkyblockCheck;
import me.valkeea.fishyaddons.util.ZoneUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;

@SuppressWarnings("squid:S6548")
public class WorldEvent {
    private boolean updateRulesNextTick = false;
    private int scoreboardDelay = 0;
    private boolean checkedIsland = false;
    private boolean checkBypass = false;
    private boolean pendingBypass = false;
    private boolean timedCheck = false;

    private static final WorldEvent INSTANCE = new WorldEvent();
    private WorldEvent() {}
    public static WorldEvent getInstance() { return INSTANCE; }

    public static void init() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> INSTANCE.onWorldLoad());
        ClientTickEvents.END_CLIENT_TICK.register(client -> INSTANCE.onTick());
    }

    public void bypass() {
        pendingBypass = true;
    }

    public void reCheck(int delay) {
        timedCheck = true;
        scoreboardDelay = delay;
    }

    public void reset() {
        timedCheck = false;
    }

    private void onWorldLoad() {
        updateRulesNextTick = true;
        scoreboardDelay = 180;
        PetInfo.onWorldLoad();
        BeaconRenderer.clearBeacons();
        CakeTimer.getInstance().onLoad();
        
        if (pendingBypass) {
            checkBypass = true;
            pendingBypass = false;
        }
        checkedIsland = checkBypass;
    }

    private void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        if (!checkedIsland && !checkBypass) {
            AreaUtils.updateIsland();
            ZoneUtils.resetRain();
            WeatherTracker.reset();            
            checkedIsland = true;
        } else if (checkBypass) {
            checkedIsland = true;
            checkBypass = false;
        }

        if (updateRulesNextTick || timedCheck) {
            if (scoreboardDelay > 0) {
                scoreboardDelay--;
            }
            if (scoreboardDelay == 0) {
                SkyblockCheck.getInstance().updateSkyblockCache();
                ZoneUtils.update();
                updateRulesNextTick = false;
                scoreboardDelay = 100;
                timedCheck = false;
            } else if (scoreboardDelay == 1) {
                ClientConnected.triggerAction();
                PetInfo.onTablistReady();
            }
        }
    }
}