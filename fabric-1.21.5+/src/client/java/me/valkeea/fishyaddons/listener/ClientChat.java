package me.valkeea.fishyaddons.listener;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.processor.ChatProcessor;
import me.valkeea.fishyaddons.processor.handlers.ChatAlertHandler;
import me.valkeea.fishyaddons.processor.handlers.ChatFormatHandler;
import me.valkeea.fishyaddons.processor.handlers.ClickEventHandler;
import me.valkeea.fishyaddons.processor.handlers.CoordinateHandler;
import me.valkeea.fishyaddons.processor.handlers.GameplayHandler;
import me.valkeea.fishyaddons.processor.handlers.HoverEventHandler;
import me.valkeea.fishyaddons.processor.handlers.StatHandler;
import me.valkeea.fishyaddons.processor.handlers.TrackerHandler;
import me.valkeea.fishyaddons.processor.handlers.XpHandler;

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
        processor.registerHandler(new ClickEventHandler());
        processor.registerHandler(new XpHandler());
        processor.registerHandler(new CoordinateHandler());
        processor.registerHandler(new ChatFormatHandler());
        
        FaEvents.GAME_MESSAGE.register(processor::onMessage);
    }
}