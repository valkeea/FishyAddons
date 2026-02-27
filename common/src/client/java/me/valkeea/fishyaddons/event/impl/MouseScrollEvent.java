package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;

public class MouseScrollEvent extends BaseEvent {
    public final double vertical;
    public final double mouseX;
    public final double mouseY;

    public MouseScrollEvent(double vertical, double mouseX, double mouseY) {
        this.vertical = vertical;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }
}
