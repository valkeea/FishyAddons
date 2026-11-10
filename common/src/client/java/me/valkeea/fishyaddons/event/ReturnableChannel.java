package me.valkeea.fishyaddons.event;

/**
 * A channel that stops dispatching once {@link BaseEvent#isConsumed()} returns true.
 * Listeners can supply a typed result that will be returned if the event is consumed.
 */
public final class ReturnableChannel<E extends BaseEvent, R> extends EventChannel<E> {
    /** Notifies listeners in priority order and returns the consumed result, or {@code defaultValue}. */
    public R fire(E event, R defaultValue) {
        return dispatcher.fireReturnable(event, listener -> listener.handle(event), defaultValue);
    }
}
