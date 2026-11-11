package me.valkeea.fishyaddons.event.impl;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

/**
 * Keyboard press, repeat or release for the Minecraft window, before vanilla handles it.
 */
public final class KeyInputEvent extends BaseEvent {
    public final Minecraft mc;
    public final @Nullable Screen screen;
    public final KeyEvent input;
    public final int action;

    public KeyInputEvent(Minecraft mc, @Nullable Screen screen, KeyEvent input, int action) {
        this.mc = mc;
        this.screen = screen;
        this.input = input;
        this.action = action;
    }
}
