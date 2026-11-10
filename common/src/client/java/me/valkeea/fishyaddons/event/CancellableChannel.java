package me.valkeea.fishyaddons.event;

/**
 * A channel that stops dispatching once {@link BaseEvent#isConsumed()} returns true.
 * Same as {@link StoppableChannel}, but events using this channel are considered cancellable.
 */
public final class CancellableChannel<E extends BaseEvent> extends EventChannel<E> {
    /** Notifies listeners in priority order, stops once the event is consumed. */
    public void fire(E event) {
        dispatcher.firePhased(event, listener -> listener.handle(event));
    }
}
