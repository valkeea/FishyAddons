package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;

class ReturnableEvent extends BaseEvent {
    /**
     * Consumes the event and sets the result to true.
     */
    public void consumeWithTrue() {
        setResult(true);
        setConsumed(true);
    }
}
