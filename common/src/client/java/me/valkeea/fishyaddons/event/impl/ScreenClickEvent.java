package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;

/**
 * Cancellable event fired when mouse is clicked on an Element in a HandledScreen.
 */
public class ScreenClickEvent extends BaseEvent {
    public final HandledScreen<?> screen;
    public final Slot hoveredSlot;
    public final Click click;
    public final boolean doubled;

    public ScreenClickEvent(HandledScreen<?> screen, Slot hoveredSlot, Click click, boolean doubled) {
        this.screen = screen;
        this.hoveredSlot = hoveredSlot;
        this.click = click;
        this.doubled = doubled;
    }
}
