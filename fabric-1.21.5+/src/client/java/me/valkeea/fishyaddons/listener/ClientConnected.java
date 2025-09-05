package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.hud.InfoDisplay;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.SkyblockCheck;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

public class ClientConnected {
    private ClientConnected() {}
    private static boolean firstLoad = false;
    private static boolean anyRecreated = false;
    private static boolean anyRestored = false;
    private static boolean pendingInfo = false;
    private static boolean pendingAlert = false;

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onClientConnected());
    }

    private static void onClientConnected() {
        refreshServerData();

        boolean r1 = FishyConfig.isRecreated();
        boolean r2 = ItemConfig.isRecreated();
        boolean r3 = me.valkeea.fishyaddons.util.ModInfo.shouldShowInfo();

        firstLoad = r1 && r2;
        anyRecreated = r1 || r2;
        anyRestored = FishyConfig.isRestored() || ItemConfig.isRestored();
        pendingInfo = r3;
        pendingAlert = firstLoad || anyRecreated || anyRestored || pendingInfo;
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
                    FishyNotis.send("One or more configuration files and their backups were missing, replaced with default.");
                }
                if (anyRestored) {
                    FishyNotis.send("One or more configuration files were corrupted and have been restored from backups.");
                }
            }
            resetFlags();
        }
    }

    private static void resetFlags() {
        ItemConfig.resetFlags();
        FishyConfig.resetFlags();
        firstLoad = false;
        anyRecreated = false;
        anyRestored = false;
        pendingInfo = false;
        pendingAlert = false;
    }

    public static void refreshServerData() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getCurrentServerEntry() != null) {
            boolean isHypixel = mc.getCurrentServerEntry().address.toLowerCase().contains("hypixel");
            SkyblockCheck.getInstance().setInHypixel(isHypixel);
        } else {
            SkyblockCheck.getInstance().setInHypixel(false);
        }
    }
}