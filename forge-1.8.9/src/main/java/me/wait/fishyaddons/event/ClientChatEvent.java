package me.wait.fishyaddons.event;

import me.wait.fishyaddons.util.AreaUtils;
import me.wait.fishyaddons.listener.WorldEventListener;
import me.wait.fishyaddons.util.SkyblockCheck;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class ClientChatEvent {
    private static final ClientChatEvent INSTANCE = new ClientChatEvent();
    private ClientChatEvent() {}
    public static ClientChatEvent getInstance() { return INSTANCE;}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(getInstance());
    }

    @SubscribeEvent
    public void onClientChat(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText();
        Pattern pattern = Pattern.compile("entered (MM )?The Catacombs");
        Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            WorldEventListener.getInstance().bypass();
            AreaUtils.setIsland("dungeon");
        }

        if (message.contains("Glacite Mineshafts")) {
            WorldEventListener.getInstance().bypass();
            AreaUtils.setIsland("mineshaft");
        }
        
        if (message.contains("You are playing on profile:")) {
            SkyblockCheck.getInstance().setInSkyblock(true);
        }
    }
}