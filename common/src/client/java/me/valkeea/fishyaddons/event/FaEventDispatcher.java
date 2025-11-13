package me.valkeea.fishyaddons.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class FaEventDispatcher<T> {
    private final Map<EventPhase, List<EventListener<T>>> phaseListeners = new EnumMap<>(EventPhase.class);

    public FaEventDispatcher() {
        for (EventPhase phase : EventPhase.values()) {
            phaseListeners.put(phase, new ArrayList<>());
        }
    }

    /** Registers a listener. */
    public void register(T listener) {
        register(listener, EventPriority.NORMAL, EventPhase.NORMAL);
    }

    /** Registers a listener with specific priority and phase. */
    public void register(T listener, EventPriority priority, EventPhase phase) {
        List<EventListener<T>> list = phaseListeners.get(phase);
        list.add(new EventListener<>(listener, priority, phase));
        list.sort(Comparator.comparing(l -> l.priority));
    }

    public void unregister(T listener) {
        for (List<EventListener<T>> list : phaseListeners.values()) {
            list.removeIf(l -> l.listener.equals(listener));
        }
    }

    public interface Invoker<T> {
        void invoke(T listener);
    }

    /** Fires listeners in one specific phase. */
    public void firePhase(EventPhase phase, BaseEvent event, Invoker<T> invoker) {
        for (EventListener<T> wrapped : phaseListeners.get(phase)) {
            invoker.invoke(wrapped.listener);
            if (event.isConsumed()) break;
        }
    }

    /** Fires all phases in order: PRE → NORMAL → POST. */
    public void firePhased(BaseEvent event, Invoker<T> invoker) {
        for (EventPhase phase : EventPhase.values()) {
            firePhase(phase, event, invoker);
            if (event.isConsumed()) break;
        }
    }

    /** Fires all phases and returns a value if the event was consumed. */
    public <R> R fireReturnable(BaseEvent event, Invoker<T> invoker, R defaultValue) {
        firePhased(event, invoker);
        return event.isConsumed() && event.getResult() != null
            ? event.getResult()
            : defaultValue;
    }
}
