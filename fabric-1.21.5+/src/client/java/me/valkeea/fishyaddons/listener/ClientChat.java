package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.processor.ChatProcessor;
import me.valkeea.fishyaddons.processor.handlers.ChatAlertHandler;
import me.valkeea.fishyaddons.processor.handlers.ChatButtonHandler;
import me.valkeea.fishyaddons.processor.handlers.CoordinateHandler;
import me.valkeea.fishyaddons.processor.handlers.GameplayHandler;
import me.valkeea.fishyaddons.processor.handlers.HoverEventHandler;
import me.valkeea.fishyaddons.processor.handlers.StatHandler;
import me.valkeea.fishyaddons.processor.handlers.TrackerHandler;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

@SuppressWarnings("squid:S6548")
public class ClientChat {
    private static final ClientChat INSTANCE = new ClientChat();
    private ClientChat() {}
    public static ClientChat getInstance() { return INSTANCE; }

    public static void init() {
        var processor = ChatProcessor.getInstance();

        processor.registerHandler(new ChatAlertHandler());
        processor.registerHandler(new GameplayHandler());
        processor.registerHandler(new StatHandler());        
        processor.registerHandler(new TrackerHandler());
        processor.registerHandler(new HoverEventHandler());
        processor.registerHandler(new CoordinateHandler());
        processor.registerHandler(new ChatButtonHandler());

        ClientReceiveMessageEvents.GAME.register(processor::onMessage);
    }
}