package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.gui.Click;

/**
 * Unreturnable event fired on mouse clicks when a HandledScreen is active.
 */
public class MouseClickEvent extends BaseEvent {
    public final Click click;

    public MouseClickEvent(Click click) {
        this.click = click;
    }
}
