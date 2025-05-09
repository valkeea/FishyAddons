package me.wait.fishyaddons.event;

import me.wait.fishyaddons.util.AreaUtils;
import me.wait.fishyaddons.listener.WorldEventListener;
import me.wait.fishyaddons.util.SkyblockCheck;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;


public class ClientChatEvent {
    private static final ClientChatEvent INSTANCE = new ClientChatEvent();
    private ClientChatEvent() {}
    public static ClientChatEvent getInstance() {
        return INSTANCE;
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ClientChatEvent());
    }

    @SubscribeEvent
    public void onClientChat(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText();
        String regex = ".*entered (MM )?The Catacombs,.*";
        
        if (message.matches(regex)) {
            WorldEventListener.getInstance().bypass();
            AreaUtils.setIsland("dungeon");
        }
        if (message.contains("Glacite Mineshafts")) {
            WorldEventListener.getInstance().bypass();
            AreaUtils.setIsland("glacite_mineshafts");

        }
        if (message.contains("You are playing on profile:")) {
            SkyblockCheck.getInstance().setInSkyblock(true);
        }
    }
}
