package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.TrackerProfiles;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class ClientDisconnected {
    private ClientDisconnected() {}
    
    public static void init() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onClientDisconnected());
    }

    private static void onClientDisconnected() {
        FishyConfig.saveBackup();
        ItemConfig.saveBackup();
        TrackerProfiles.backupAll();
        TrackerProfiles.saveProfile();
    }
}