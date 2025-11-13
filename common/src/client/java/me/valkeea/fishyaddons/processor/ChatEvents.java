package me.valkeea.fishyaddons.processor;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.ScCatchEvent;
import me.valkeea.fishyaddons.processor.AnalysisCoordinator.AnalysisResult;

public class ChatEvents {
    private ChatEvents() {}

    public static void dispatch(ChatMessageContext context) {
        if (context.isSeaCreatureMessage()) scEvent(context.getAnalysisResult());
    }

    private static void scEvent(AnalysisResult result) {
        var event = new ScCatchEvent(result);
        FaEvents.SEA_CREATURE_CATCH.firePhased(event, listener -> listener.onScCatch(event));
    }    

}
