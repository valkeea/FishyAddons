package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.handler.ClientPing;
import me.valkeea.fishyaddons.handler.ChatTimers;
import me.valkeea.fishyaddons.config.FishyConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientTick {
    private ClientTick() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if ((FishyConfig.getState(me.valkeea.fishyaddons.config.Key.HUD_PING_ENABLED, false)) && 
                client.player != null && client.world != null && client.world.getTime() % 60 == 0) {
                ClientPing.send();
            }
            if ((FishyConfig.getState(me.valkeea.fishyaddons.config.Key.BEACON_ALARM, false)) && client.player != null &&
            client.world != null && client.world.getTime() % 10 == 0) {
                ChatTimers.getInstance().checkTimerAlert();
            }
            // clean up entity tracker every 30 seconds (100 ticks)
            if (client.world != null && client.world.getTime() % 600 == 0) {
                me.valkeea.fishyaddons.util.EntityTracker.cleanup();
                me.valkeea.fishyaddons.util.EntityTracker.cleanVal();
            }
        });
    }    
}
