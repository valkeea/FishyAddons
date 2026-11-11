package me.valkeea.fishyaddons.event.impl;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonInfo;

/** Mouse button transition, before vanilla handles it. */
public final class MouseInputEvent extends BaseEvent {
    public final Minecraft mc;
    public final @Nullable Screen screen;
    public final MouseButtonInfo input;
    public final int action;

    public MouseInputEvent(Minecraft mc, @Nullable Screen screen, MouseButtonInfo input, int action) {
        this.mc = mc;
        this.screen = screen;
        this.input = input;
        this.action = action;
    }
}
