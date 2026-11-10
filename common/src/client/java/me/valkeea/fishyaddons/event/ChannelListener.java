package me.valkeea.fishyaddons.event;

@FunctionalInterface
public interface ChannelListener<E extends BaseEvent> {
    void handle(E event);
}
