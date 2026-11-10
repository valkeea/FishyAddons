package me.valkeea.fishyaddons.event;

/**
 * Read-only observation channel: every listener sees every event, and consumption has no effect.
 */
public final class ObservationChannel<E extends BaseEvent> extends EventChannel<E> {
    /** Notifies every listener in priority order. */
    public void broadcast(E event) {
        dispatcher.fireAll(listener -> listener.handle(event));
    }
}
