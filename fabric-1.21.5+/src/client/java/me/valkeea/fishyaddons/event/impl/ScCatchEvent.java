package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import me.valkeea.fishyaddons.processor.AnalysisCoordinator.AnalysisResult;
import me.valkeea.fishyaddons.tracker.ActivityMonitor;

public class ScCatchEvent extends BaseEvent {
    public final boolean wasDh;
    public final String seaCreatureId;

    public ScCatchEvent(AnalysisResult context) {
        this.wasDh = context.isDoubleHook();
        this.seaCreatureId = context.getSeaCreatureId();
        ActivityMonitor.getInstance().recordActivity(ActivityMonitor.Currently.FISHING);
    }
}
