package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;

public class ScreenClickEvent extends BaseEvent {
    public final HandledScreen<?> screen;
    public final Slot hoveredSlot;
    public final double mouseX;
    public final double mouseY;
    public final int button;

    public ScreenClickEvent(HandledScreen<?> screen, Slot hoveredSlot, double mouseX, double mouseY, int button) {
        this.screen = screen;
        this.hoveredSlot = hoveredSlot;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.button = button;
    }
}
