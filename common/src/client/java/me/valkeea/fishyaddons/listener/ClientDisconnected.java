package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.feature.filter.FilterConfig;
import me.valkeea.fishyaddons.feature.waypoints.ChainConfig;
import me.valkeea.fishyaddons.tracker.monitoring.ActivityMonitor;
import me.valkeea.fishyaddons.tracker.profit.TrackerProfiles;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class ClientDisconnected {
    private ClientDisconnected() {}
    
    public static void init() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onClientDisconnected());
    }

    private static void onClientDisconnected() {

        GameMode.leftSkyblock();
        
        Config.saveBackup();
        FilterConfig.saveBackup();
        ChainConfig.saveBackup();
        TrackerProfiles.backupAll();
        TrackerProfiles.saveProfile();
        ActivityMonitor.getInstance().forceSave();
    }
}
