package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import me.valkeea.fishyaddons.processor.AnalysisCoordinator.AnalysisResult;
import me.valkeea.fishyaddons.tracker.monitoring.ActivityMonitor;
import me.valkeea.fishyaddons.tracker.monitoring.Currently;

public class ScCatchEvent extends BaseEvent {
    public final boolean wasDh;
    public final String seaCreatureId;

    public ScCatchEvent(AnalysisResult context) {
        this.wasDh = context.isDoubleHook();
        this.seaCreatureId = context.getSeaCreatureId();
        ActivityMonitor.getInstance().recordActivity(Currently.FISHING);
    }
}
