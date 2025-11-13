package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.feature.skyblock.CakeTimer;
import me.valkeea.fishyaddons.feature.waypoints.ChainConfig;
import me.valkeea.fishyaddons.feature.waypoints.WaypointChains;
import me.valkeea.fishyaddons.hud.elements.custom.InfoDisplay;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;

public class ClientConnected {
    private ClientConnected() {}

    private static boolean firstLoad = false;
    private static boolean anyRecreated = false;
    private static boolean anyRestored = false;
    private static boolean pendingInfo = false;
    private static boolean pendingAlert = false;

    public static void init() {
        ClientLoginConnectionEvents.INIT.register((handler, client) -> onClientConnected());
    }

    private static void onClientConnected() {

        boolean r1 = FishyConfig.isRecreated();
        boolean r2 = ItemConfig.isRecreated();
        boolean r3 = ChainConfig.isRecreated();
        boolean a1 = me.valkeea.fishyaddons.util.ModInfo.shouldShowInfo();

        firstLoad = r1 && r2 && r3;
        anyRecreated = r1 || r2 || r3;
        anyRestored = FishyConfig.isRestored() || ItemConfig.isRestored() || ChainConfig.isRestored();
        pendingInfo = a1;
        pendingAlert = firstLoad || anyRecreated || anyRestored || pendingInfo;

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
                if (anyRecreated) {
                    FishyNotis.notice("One or more configuration files and their backups were missing, replaced with default.");
                }
                if (anyRestored) {
                    FishyNotis.notice("One or more configuration files were corrupted and have been restored from backups.");
                }
            }
            resetFlags();
        }
    }

    private static void resetFlags() {
        ItemConfig.resetFlags();
        FishyConfig.resetFlags();
        ChainConfig.resetFlags();
        firstLoad = false;
        anyRecreated = false;
        anyRestored = false;
        pendingInfo = false;
        pendingAlert = false;
    }

    private static void onInitialLoad() {
        WaypointChains.onConnect();
        CakeTimer.getInstance().onLoad();        
    }
}
