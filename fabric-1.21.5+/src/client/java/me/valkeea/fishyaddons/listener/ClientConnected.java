package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.config.*;
import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.SkyblockCheck;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

public class ClientConnected {
    private ClientConnected() {}
    private static boolean firstLoad = false;
    private static boolean anyRecreated = false;
    private static boolean anyRestored = false;
    private static boolean pendingAlert = false;

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> onClientConnected());
    }

    private static void onClientConnected() {
        refreshServerData();

        boolean r1 = FishyConfig.isRecreated();
        //boolean r2 = TextureConfig.isRecreated();
        boolean r3 = ItemConfig.isRecreated();

        firstLoad = r1 && r3;// && r2; 
        anyRecreated = r1 || r3;// || r2;
        anyRestored = FishyConfig.isRestored() || TextureConfig.isRestored() || ItemConfig.isRestored();
        pendingAlert = firstLoad || anyRecreated || anyRestored;
    }

    public static void triggerAction() {
        if (pendingAlert) {
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
        TextureConfig.resetFlags();
        FishyConfig.resetFlags();
        firstLoad = false;
        anyRecreated = false;
        anyRestored = false;
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