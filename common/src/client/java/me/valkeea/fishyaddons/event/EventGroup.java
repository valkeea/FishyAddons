package me.valkeea.fishyaddons.event;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.function.Supplier;

public final class EventGroup {
    private final List<Subscription> active = new ArrayList<>();
    private final Logger logger = Logger.getLogger(EventGroup.class.getName());
    private boolean enabled;

    /**Adds a subscription to this scope. */
    public void add(Subscription subscription) {
        active.add(subscription);
    }

    /**
     * {@link #add(Subscription)} Batch version.
     * Subscriptions are passed as a supplier to prevent eager evaluation.
     * <p>
     * Example:
     * <pre>{@code
     * eventGroup.addAll(() -> EVENT.register(...), () -> EVENT.register(...));
     * }</pre>
     */
    @SafeVarargs
    public final void addAll(Supplier<Subscription>... registrations) {
        for (var registration : registrations) {
            add(registration.get());
        }
    }

    /**
     * Transitions to {@code enabled} if it differs from the current state.
     * Enabling runs {@code onEnable}, which is expected to populate this scope via {@link #add}.
     * Disabling closes every subscription added since the last enable.
     * <p>
     * Calling this with the current state is a no-op.
     */
    public void subscribe(boolean newState, Runnable onEnable) {
        if (newState == this.enabled) return;

        if (newState) {
            try {
                onEnable.run();
                this.enabled = newState;
            } catch (RuntimeException e) {
                logger.warning("Exception occurred during onEnable: " + e.getMessage());
                close();
            }

        } else close();
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Closes every active subscription, if any exist. */
    public void close() {
        for (Subscription subscription : active) {
            subscription.close();
        }
        active.clear();
        enabled = false;
    }
}
