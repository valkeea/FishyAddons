package me.valkeea.fishyaddons.event;

/**
 * Each type adds exactly one dispatch, matching what consuming the event means on that channel.
 */
public abstract sealed class EventChannel<E extends BaseEvent> 
    permits ObservationChannel, StoppableChannel, CancellableChannel, ReturnableChannel {
    protected final FaEventDispatcher<ChannelListener<E>> dispatcher = new FaEventDispatcher<>();

    /** Registers a listener. */
    public Subscription register(ChannelListener<E> listener) {
        return dispatcher.register(listener);
    }

    /** Registers a listener with specific priority. */
    public Subscription register(ChannelListener<E> listener, EventPriority priority) {
        return dispatcher.register(listener, priority);
    }
}
