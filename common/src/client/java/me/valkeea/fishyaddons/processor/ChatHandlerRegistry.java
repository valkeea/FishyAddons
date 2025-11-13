package me.valkeea.fishyaddons.processor;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.processor.handlers.*;

@SuppressWarnings("squid:S6548")
public class ChatHandlerRegistry {
    private ChatHandlerRegistry() {}    
    private static final ChatHandlerRegistry INSTANCE = new ChatHandlerRegistry();
    public static ChatHandlerRegistry getInstance() { return INSTANCE; }

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
