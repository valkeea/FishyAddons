package me.valkeea.fishyaddons.processor;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.ScCatchEvent;
import me.valkeea.fishyaddons.processor.AnalysisCoordinator.AnalysisResult;

public class ChatEvents {
    private ChatEvents() {}

    public static void dispatch(ChatMessageContext ctx) {
        if (ctx.isSeaCreatureMessage()) scEvent(ctx.getAnalysisResult());
    }

    private static void scEvent(AnalysisResult r) {
        var event = new ScCatchEvent(r);
        FaEvents.SEA_CREATURE_CATCH.firePhased(event, listener -> listener.onScCatch(event));
    }    

}
