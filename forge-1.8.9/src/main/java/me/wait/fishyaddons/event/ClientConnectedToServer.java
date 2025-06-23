package me.wait.fishyaddons.event;

import me.wait.fishyaddons.config.UUIDConfigHandler;
import me.wait.fishyaddons.config.TextureConfig;
import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.util.FishyNotis;
import me.wait.fishyaddons.util.SkyblockCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;

public class ClientConnectedToServer {
    private static boolean firstLoad = false;
    private static boolean anyRecreated = false;
    private static boolean anyRestored = false;
    private static boolean pendingAlert = false;

    private static Minecraft getMc() {
        return Minecraft.getMinecraft();
    }    

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ClientConnectedToServer());
    }

    @SubscribeEvent
    public void onClientConnected(ClientConnectedToServerEvent event) {
        refreshServerData();

        boolean r1 = ConfigHandler.isRecreated();
        boolean r2 = TextureConfig.isRecreated();
        boolean r3 = UUIDConfigHandler.isRecreated();

        firstLoad = r1 && r2 && r3;

        anyRecreated = r1 || r2 || r3;

        anyRestored = ConfigHandler.isRestored() || TextureConfig.isRestored() || UUIDConfigHandler.isRestored();

        pendingAlert = firstLoad || anyRecreated || anyRestored;
    }

    public static void triggerAction() {
        if (pendingAlert) {

            if (firstLoad) {
                FishyNotis.guideNoti();
            } else {
                if (anyRecreated) {
                    FishyNotis.send(
                        EnumChatFormatting.GRAY + "One or more configuration files and their backups were missing, replaced with default.");
                }
                if (anyRestored) {
                    FishyNotis.send(
                        EnumChatFormatting.GRAY + "One or more configuration files were corrupted and have been restored from backups.");
                }
            }
            resetFlags();
        }
    }

    private static void resetFlags() {
        UUIDConfigHandler.resetFlags();
        TextureConfig.resetFlags();
        ConfigHandler.resetFlags();
        firstLoad = false;
        anyRecreated = false;
        anyRestored = false;
        pendingAlert = false;
    }

    public static void refreshServerData() {
        if (getMc().getCurrentServerData() != null) {
            getMc().getCurrentServerData().serverIP.toLowerCase().contains("hypixel");
            SkyblockCheck.getInstance().setInHypixel(true);
        } else {
            SkyblockCheck.getInstance().setInHypixel(false);
        }
    }    
}