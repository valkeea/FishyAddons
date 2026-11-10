package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.input.MouseButtonEvent;

/** Mouse PRESS input in a container screen. */
public final class HudClickEvent extends BaseEvent {
    public final MouseButtonEvent click;

    public HudClickEvent(MouseButtonEvent click) {
        this.click = click;
    }
}
