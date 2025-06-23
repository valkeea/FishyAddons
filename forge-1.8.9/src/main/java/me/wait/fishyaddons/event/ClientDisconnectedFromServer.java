package me.wait.fishyaddons.event;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.config.TextureConfig;
import me.wait.fishyaddons.config.UUIDConfigHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;

public class ClientDisconnectedFromServer {

    public static void init(){
        MinecraftForge.EVENT_BUS.register(new ClientDisconnectedFromServer());
    }

    @SubscribeEvent
    public void onClientDisconnected(ClientDisconnectionFromServerEvent event) {

        ConfigHandler.saveBackup();
        TextureConfig.saveBackup();
        UUIDConfigHandler.saveBackup();
    }
}
