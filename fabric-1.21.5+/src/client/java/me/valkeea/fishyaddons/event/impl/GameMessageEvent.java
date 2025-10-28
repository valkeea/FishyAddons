package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.text.Text;

public class GameMessageEvent extends BaseEvent {
    public final Text message;
    public final boolean overlay;

    public GameMessageEvent(Text message, boolean overlay) {
        this.message = message;
        this.overlay = overlay;
    }
}
