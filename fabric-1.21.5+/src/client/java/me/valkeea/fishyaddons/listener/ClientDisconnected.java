package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.config.FilterConfig;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.StatConfig;
import me.valkeea.fishyaddons.config.TrackerProfiles;
import me.valkeea.fishyaddons.tracker.ActivityMonitor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class ClientDisconnected {
    private ClientDisconnected() {}
    
    public static void init() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onClientDisconnected());
    }

    private static void onClientDisconnected() {
        FishyConfig.saveBackup();
        ItemConfig.saveBackup();
        StatConfig.saveBackup();
        FilterConfig.saveBackup();
        TrackerProfiles.backupAll();
        TrackerProfiles.saveProfile();
        ActivityMonitor.getInstance().forceSave();
    }
}