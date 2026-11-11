package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/** Fired before the active screen is changed. */
public final class ScreenChangeEvent extends BaseEvent {
    public final @Nullable Screen previous;
    public final @Nullable Screen next;

    public ScreenChangeEvent(@Nullable Screen previous, @Nullable Screen next) {
        this.previous = previous;
        this.next = next;
    }
}
