package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.config.FilterConfig;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.StatConfig;
import me.valkeea.fishyaddons.config.TrackerProfiles;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.feature.item.animations.HeldItems;
import me.valkeea.fishyaddons.feature.waypoints.ChainConfig;
import me.valkeea.fishyaddons.tracker.ActivityMonitor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class ClientDisconnected {
    private ClientDisconnected() {}
    
    public static void init() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onClientDisconnected());
    }

    private static void onClientDisconnected() {

        GameMode.sbEvent(false);

        FishyConfig.saveBackup();
        ItemConfig.saveBackup();
        StatConfig.saveBackup();
        FilterConfig.saveBackup();
        ChainConfig.saveBackup();
        TrackerProfiles.backupAll();
        TrackerProfiles.saveProfile();
        HeldItems.saveConfig();
        ActivityMonitor.getInstance().forceSave();
    }
}
