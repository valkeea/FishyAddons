package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.network.chat.Component;

public class GameMessageEvent extends BaseEvent {
    public final Component message;
    public final boolean overlay;

    public GameMessageEvent(Component message, boolean overlay) {
        this.message = message;
        this.overlay = overlay;
    }
}
