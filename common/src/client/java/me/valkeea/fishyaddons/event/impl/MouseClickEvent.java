package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * Unreturnable event fired on mouse clicks when a HandledScreen is active.
 */
public class MouseClickEvent extends BaseEvent {
    public final MouseButtonEvent click;

    public MouseClickEvent(MouseButtonEvent click) {
        this.click = click;
    }
}
