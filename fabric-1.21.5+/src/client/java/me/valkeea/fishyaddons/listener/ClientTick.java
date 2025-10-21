package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.handler.ChatTimers;
import me.valkeea.fishyaddons.handler.NetworkMetrics;
import me.valkeea.fishyaddons.tracker.ActivityMonitor;
import me.valkeea.fishyaddons.tracker.SkillTracker;
import me.valkeea.fishyaddons.util.ModInfo;
import me.valkeea.fishyaddons.util.NearbyEntities;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientTick {
    private ClientTick() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            
            ActivityMonitor.getInstance().tick();
            
            if (NetworkMetrics.isOn() && client.world.getTime() % 40 == 0) {
                NetworkMetrics.send();
            }
            
            if (ChatTimers.getInstance().isBeaconAlarmOn() && client.world.getTime() % 10 == 0) {
                ChatTimers.getInstance().checkTimerAlert();
            }
            
            NearbyEntities.tick();
            SkillTracker.getInstance().tick();            
            ModInfo.tick();

            if (client.world != null && client.world.getTime() % 600 == 0) {
                me.valkeea.fishyaddons.tracker.InventoryTracker.cleanup();
            }
        });
    }    
}