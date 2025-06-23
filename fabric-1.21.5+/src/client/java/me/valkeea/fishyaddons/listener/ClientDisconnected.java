package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.config.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class ClientDisconnected {
    private ClientDisconnected() {}
    public static void init() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onClientDisconnected());
    }

    private static void onClientDisconnected() {
        FishyConfig.saveBackup();
        TextureConfig.saveBackup();
        ItemConfig.saveBackup();
    }
}