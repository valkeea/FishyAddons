package me.valkeea.fishyaddons.event;

/**
 * A handle to a single listener registration on a {@link FaEventDispatcher}.
 */
@FunctionalInterface
public interface Subscription {
    Subscription NOOP = () -> { };

    void close();
}
