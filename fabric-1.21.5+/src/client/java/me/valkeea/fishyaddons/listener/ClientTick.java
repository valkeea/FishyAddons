package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.handler.ChatTimers;
import me.valkeea.fishyaddons.handler.ClientPing;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientTick {
    private ClientTick() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ClientPing.isOn() && client.player != null && client.world != null && client.world.getTime() % 60 == 0) {
                ClientPing.send();
            }
            if (ChatTimers.getInstance().isBeaconAlarmOn() && client.player != null && client.world != null && client.world.getTime() % 10 == 0) {
                ChatTimers.getInstance().checkTimerAlert();
            }
            
            me.valkeea.fishyaddons.util.ModInfo.tick();

            if (client.world != null && client.world.getTime() % 600 == 0) {
                me.valkeea.fishyaddons.util.EntityTracker.cleanup();
                me.valkeea.fishyaddons.util.EntityTracker.cleanVal();
                me.valkeea.fishyaddons.tracker.InventoryTracker.cleanup();
            }
        });
    }    
}