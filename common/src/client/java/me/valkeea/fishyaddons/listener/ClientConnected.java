package me.valkeea.fishyaddons.listener;

import java.util.HashSet;
import java.util.Set;

import me.valkeea.fishyaddons.feature.filter.RuleFactory;
import me.valkeea.fishyaddons.feature.skyblock.timer.CakeTimer;
import me.valkeea.fishyaddons.feature.waypoints.ChainConfig;
import me.valkeea.fishyaddons.feature.waypoints.WaypointChains;
import me.valkeea.fishyaddons.hud.elements.custom.InfoDisplay;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.config.BaseConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;

public class ClientConnected {
    private ClientConnected() {}

    private static boolean firstLoad = false;
    private static boolean anyRecreated = false;
    private static boolean anyRestored = false;
    private static boolean rulesCorrupted = false;
    private static boolean pendingInfo = false;
    private static boolean pendingAlert = false;
    private static boolean migrationFailed = false;
    private static Set<String> restored = new HashSet<>();
    private static Set<String> recreated = new HashSet<>();

    public static void init() {
        ClientLoginConnectionEvents.INIT.register((handler, client) -> onClientConnected());
    }

    private static void onClientConnected() {

        var registered = Config.getConfigs().values();
        for (BaseConfig file : registered) {
            if (file != null) {
                if (file.isRestored()) restored.add(file.getConfigName());
                if (file.isRecreated()) recreated.add(file.getConfigName());
            }
        }

        int amount = registered.size();

        firstLoad = recreated.size() == amount;
        anyRecreated = !recreated.isEmpty() || RuleFactory.isRecreated() || ChainConfig.isRecreated();
        anyRestored = !restored.isEmpty() || ChainConfig.isRestored();
        rulesCorrupted = RuleFactory.isCorrupted();
        pendingInfo = me.valkeea.fishyaddons.util.ModInfo.shouldShowInfo();
        pendingAlert = firstLoad || anyRecreated || anyRestored || rulesCorrupted || pendingInfo;

        onInitialLoad();
    }

    public static void triggerAction() {
        if (pendingAlert) {
            if (pendingInfo) {
                InfoDisplay.getInstance().show(me.valkeea.fishyaddons.util.ModInfo.getInfoMessage());
            }            
            if (firstLoad) {
                FishyNotis.guideNoti();
            } else {
                if (migrationFailed) {
                    migrationFailed();
                    return;
                }
                if (anyRecreated) recreatedMsg();
                if (anyRestored) restoredMsg();
                if (rulesCorrupted) rulesCorruptedMsg();
            }
            resetFlags();
        }
    }

    private static void recreatedMsg() {
        String files = String.join(", ", recreated);
        FishyNotis.notice("""
            The following configuration files were missing and have been replaced with defaults:
            """ + files + ". Any existing backups were corrupted or missing."
        );
    }

    private static void restoredMsg() {
        FishyNotis.notice("""
            The following configuration files were corrupted and have been restored from backup:
            """ + String.join(", ", restored) + "."
        );
    }

    private static void rulesCorruptedMsg() {
        FishyNotis.notice("""
            Detected corruption in config/fishyaddons/data/sea_creatures.json.
            You can either fix the format or delete the file to regenerate it.
            """
        );
    }

    private static void migrationFailed() {
        FishyNotis.notice("""
            §4CRITICAL: Migration failed for one or more config files.
            If you were using an older version of FishyAddons, some settings may not have been migrated.
            You can either re-config your mod, post an issue or downgrade to an older version to preserve your settings
            until the migration issues are resolved.
            Sorry for the inconvenience!
            """
        );
    }

    public static void notifyMigrationIssues() {
        migrationFailed = true;
    }

    private static void resetFlags() {
        Config.resetFlags();
        ChainConfig.resetFlags();
        RuleFactory.resetFlags();
        firstLoad = false;
        anyRecreated = false;
        anyRestored = false;
        rulesCorrupted = false;
        pendingInfo = false;
        pendingAlert = false;
        migrationFailed = false;
    }

    private static void onInitialLoad() {
        WaypointChains.onConnect();
        CakeTimer.getInstance().onLoad();        
    }
}
