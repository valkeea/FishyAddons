package me.valkeea.fishyaddons.event;

final class EventListener<T> {
    final T listener;
    final EventPriority priority;

    EventListener(T listener, EventPriority priority) {
        this.listener = listener;
        this.priority = priority;
    }
}
