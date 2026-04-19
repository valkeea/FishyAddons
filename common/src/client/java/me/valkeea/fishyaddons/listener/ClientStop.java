package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.feature.skyblock.PetInfo.ActivePet;
import me.valkeea.fishyaddons.tracker.PriceUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class ClientStop {

    public static void init () {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            saveDiana();
            saveSlayer();
            saveScStats();
            saveScData();
            saveCakeTimer();
            saveEffectTimers();
            saveCollections();
            shutdown();
        });
    }

    private static void saveDiana() {
        try {
            if (me.valkeea.fishyaddons.tracker.DianaStats.loaded()) {
                me.valkeea.fishyaddons.tracker.DianaStats.getInstance().save();
            }
        } catch (Exception e) {
            System.err.println("ClientStop: Failed to save DianaStats: " + e.getMessage());
        }
    }

    private static void saveSlayer() {
        try {
            if (me.valkeea.fishyaddons.tracker.SlayerStats.loaded()) {
                me.valkeea.fishyaddons.tracker.SlayerStats.getInstance().save();
            }
        } catch (Exception e) {
            System.err.println("ClientStop: Failed to save SlayerStats: " + e.getMessage());
        }
    }

    private static void saveScStats() {
        try {
            if (me.valkeea.fishyaddons.tracker.fishing.ScStats.isEnabled()) {
                me.valkeea.fishyaddons.tracker.fishing.ScStats.getInstance().save();
            }
        } catch (Exception e) {
            System.err.println("ClientStop: Failed to save ScStats: " + e.getMessage());
        }
    }

    private static void saveScData() {
        try {
            if (me.valkeea.fishyaddons.tracker.fishing.ScData.isEnabled()) {
                me.valkeea.fishyaddons.tracker.fishing.ScData.getInstance().save();
            }
        } catch (Exception e) {
            System.err.println("ClientStop: Failed to save ScData: " + e.getMessage());
        }
    }

    private static void saveCakeTimer() {
        try {
            if (me.valkeea.fishyaddons.feature.skyblock.timer.CakeTimer.isEnabled()) {
            me.valkeea.fishyaddons.feature.skyblock.timer.CakeTimer.getInstance().shutdown();
            }
        } catch (Exception e) {
            System.err.println("ClientStop: Failed to shutdown CakeTimer: " + e.getMessage());
        }
    }

    private static void saveEffectTimers() {
        try {
            if (me.valkeea.fishyaddons.vconfig.api.Config.get(me.valkeea.fishyaddons.vconfig.api.BooleanKey.HUD_EFFECTS_ENABLED)) {
                me.valkeea.fishyaddons.feature.skyblock.timer.EffectTimers.getInstance().shutdown();
            }
        } catch (Exception e) {
            System.err.println("ClientStop: Failed to shutdown EffectTimers: " + e.getMessage());
        }
    }

    private static void saveCollections() {
        try {
            if (me.valkeea.fishyaddons.tracker.collection.CollectionTracker.isEnabled()) {
                me.valkeea.fishyaddons.tracker.collection.CollectionTracker.shutdown();
            }
        } catch (Exception e) {
            System.err.println("ClientStop: Failed to shutdown CollectionTracker: " + e.getMessage());
        }
    }

    private static void shutdown() {
        PriceUtil.shutdown();
        ActivePet.shutdown();
    }

    private ClientStop() {}
}
