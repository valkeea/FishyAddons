package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SpawnData;
import me.valkeea.fishyaddons.feature.qol.NetworkMetrics;
import me.valkeea.fishyaddons.feature.skyblock.PetInfo;
import me.valkeea.fishyaddons.feature.skyblock.WeatherTracker;
import me.valkeea.fishyaddons.feature.waypoints.TempWaypoint;
import me.valkeea.fishyaddons.util.TabScanner;
import me.valkeea.fishyaddons.util.ZoneUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.minecraft.client.MinecraftClient;

@SuppressWarnings("squid:S6548")
public class WorldEvent {
    private static final WorldEvent INSTANCE = new WorldEvent();
    public static WorldEvent getInstance() { return INSTANCE; }

    private boolean updateRulesNextTick = false;
    private boolean checkedIsland = false;
    private boolean checkBypass = false;
    private boolean pendingBypass = false;
    private boolean timedCheck = false;

    private int scoreboardDelay = 0;
    private MinecraftClient mc;

    public static void init() {
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> INSTANCE.onWorldLoad());
        ClientTickEvents.END_CLIENT_TICK.register(client -> INSTANCE.onTick());
        INSTANCE.mc = MinecraftClient.getInstance();
    }

    private void onWorldLoad() {

        updateRulesNextTick = true;
        scoreboardDelay = 180;

        PetInfo.onWorldLoad();
        SkyblockAreas.reset();
        ZoneUtils.resetDungeon();
        TempWaypoint.clearBeacons();
        NetworkMetrics.reset();
        TabScanner.reset();
        
        if (pendingBypass) {
            checkBypass = true;
            pendingBypass = false;
        }

        checkedIsland = checkBypass;
    }

    private void onTick() {
        if (mc.world == null || mc.player == null) return;

        if (!checkedIsland && !checkBypass) {
            SpawnData.updateIsland();
            ZoneUtils.resetRain();
            WeatherTracker.reset();            
            checkedIsland = true;

        } else if (checkBypass) {
            checkedIsland = true;
            checkBypass = false;
        }

        if (updateRulesNextTick || timedCheck) {
            if (scoreboardDelay > 0) scoreboardDelay--;

            if (scoreboardDelay == 0) {
                timedCheck = false;
                GameMode.getInstance().updateSkyblockStatus();
                ZoneUtils.update();
                updateRulesNextTick = false;
                scoreboardDelay = 100;

            } else if (scoreboardDelay == 1) {
                ClientConnected.triggerAction();
                PetInfo.onTablistReady();
            }
        }
    }

    public void bypass() {
        pendingBypass = true;
    }

    public void reset() {
        timedCheck = false;
    }
    
    public void reCheck(int delay) {
        timedCheck = true;
        scoreboardDelay = delay;
    }

    private WorldEvent() {}
}
