package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.feature.qol.NetworkMetrics;
import me.valkeea.fishyaddons.feature.skyblock.CatchAlert;
import me.valkeea.fishyaddons.feature.skyblock.timer.ChatTimers;
import me.valkeea.fishyaddons.tracker.SkillTracker;
import me.valkeea.fishyaddons.tracker.collection.CollectionTracker;
import me.valkeea.fishyaddons.tracker.monitoring.ActivityMonitor;
import me.valkeea.fishyaddons.tracker.profit.InventoryTracker;
import me.valkeea.fishyaddons.util.ModInfo;
import me.valkeea.fishyaddons.util.NearbyEntities;
import me.valkeea.fishyaddons.util.ServerCommand;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class ClientTick {
    private ClientTick() {}

    public static void init() {
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            var time = client.world.getTime();
            
            ServerCommand.tick();
            ActivityMonitor.getInstance().tick();
            
            if (time % 40 == 0 && NetworkMetrics.isOn()) {
                NetworkMetrics.send();
            }
            
            if (time % 10 == 0 && ChatTimers.getInstance().isBeaconAlarmOn()) {
                ChatTimers.getInstance().checkTimerAlert();
            }
            
            NearbyEntities.tick();
            SkillTracker.getInstance().tick();
            CatchAlert.tick();
            ModInfo.tick();

            if (CollectionTracker.isEnabled()) CollectionTracker.tick();
            if (time % 600 == 0) InventoryTracker.cleanup();
        });
    }    
}
