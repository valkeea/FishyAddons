package me.valkeea.fishyaddons.event.impl;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;

/** Returnable mouse PRESS input in a container screen. */
public final class ContainerClickEvent extends ReturnableEvent {
    public final AbstractContainerScreen<?> screen;
    public final Slot hoveredSlot;
    public final MouseButtonEvent click;
    public final boolean doubled;

    public ContainerClickEvent(AbstractContainerScreen<?> screen, Slot hoveredSlot, MouseButtonEvent click, boolean doubled) {
        this.screen = screen;
        this.hoveredSlot = hoveredSlot;
        this.click = click;
        this.doubled = doubled;
    }
}
