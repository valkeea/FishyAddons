package me.valkeea.fishyaddons.event.impl;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.inventory.Slot;

/**
 * A key press/repeat input in a container screen.
 */
public final class ContainerKeyInputEvent extends ReturnableEvent {
    public final Minecraft mc;
    public final AbstractContainerScreen<?> screen;
    public final @Nullable Slot hoveredSlot;
    public final KeyEvent input;

    public ContainerKeyInputEvent(Minecraft mc, AbstractContainerScreen<?> screen,
            @Nullable Slot hoveredSlot, KeyEvent input) {
        this.mc = mc;
        this.screen = screen;
        this.hoveredSlot = hoveredSlot;
        this.input = input;
    }
}
