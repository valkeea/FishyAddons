package me.valkeea.fishyaddons.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class FaEventDispatcher<T> {
    private final List<EventListener<T>> listeners = new ArrayList<>();

    /** Registers a listener. */
    public Subscription register(T listener) {
        return register(listener, EventPriority.NORMAL);
    }

    /** Registers a listener with specific priority. */
    public Subscription register(T listener, EventPriority priority) {
        EventListener<T> wrapped = new EventListener<>(listener, priority);
        listeners.add(wrapped);
        listeners.sort(Comparator.comparing(l -> l.priority));
        return () -> listeners.remove(wrapped);
    }

    interface Invoker<T> {
        void invoke(T listener);
    }

    /** Fires all listeners in priority order without cancellation. */
    public void fireAll(Invoker<T> invoker) {
        for (EventListener<T> wrapped : listeners) {
            invoker.invoke(wrapped.listener);
        }
    }

    /** Fires listeners in priority order; stops once the event is consumed. */
    public void firePhased(BaseEvent event, Invoker<T> invoker) {
        for (EventListener<T> wrapped : listeners) {
            invoker.invoke(wrapped.listener);
            if (event.isConsumed()) break;
        }
    }

    /** Fires in priority order and returns a value if the event was consumed. */
    public <R> R fireReturnable(BaseEvent event, Invoker<T> invoker, R defaultValue) {
        firePhased(event, invoker);
        return event.isConsumed() && event.getResult() != null
            ? event.getResult()
            : defaultValue;
    }
}
