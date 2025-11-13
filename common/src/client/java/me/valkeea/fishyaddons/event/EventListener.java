package me.valkeea.fishyaddons.event;

public class EventListener<T> {
    public final T listener;
    public final EventPriority priority;
    public final EventPhase phase;

    public EventListener(T listener, EventPriority priority, EventPhase phase) {
        this.listener = listener;
        this.priority = priority;
        this.phase = phase;
    }
}
