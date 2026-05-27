package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;

/**
 * Cancellable event fired when mouse is clicked on an Element in a HandledScreen.
 */
public class ScreenClickEvent extends BaseEvent {
    public final AbstractContainerScreen<?> screen;
    public final Slot hoveredSlot;
    public final MouseButtonEvent click;
    public final boolean doubled;

    public ScreenClickEvent(AbstractContainerScreen<?> screen, Slot hoveredSlot, MouseButtonEvent click, boolean doubled) {
        this.screen = screen;
        this.hoveredSlot = hoveredSlot;
        this.click = click;
        this.doubled = doubled;
    }
}
