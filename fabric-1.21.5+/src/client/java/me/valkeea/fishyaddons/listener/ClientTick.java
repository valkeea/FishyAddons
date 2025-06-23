package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.handler.ClientPing;
import me.valkeea.fishyaddons.handler.ChatTimers;
import me.valkeea.fishyaddons.config.FishyConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientTick {
    private ClientTick() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if ((FishyConfig.getState("pingHud", false)) && client.player != null &&
            client.world != null && client.world.getTime() % 60 == 0) {
                ClientPing.send();
            }
            if ((FishyConfig.getState("beaconAlarm", false)) && client.player != null &&
            client.world != null && client.world.getTime() % 10 == 0) {
                ChatTimers.getInstance().checkTimerAlert();
            }
        });
    }    
}
