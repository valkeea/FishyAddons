package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.text.Text;

public class ScreenCloseEvent extends BaseEvent {
    public final Text title;
    public final String titleString;

    public ScreenCloseEvent(Text title) {
        this.title = title;
        this.titleString = title.getString();
    }

    /**
     * Check if title matches a pattern (case-insensitive)
     */
    public boolean titleContains(String pattern) {
        return titleString.toLowerCase().contains(pattern.toLowerCase());
    }

    /**
     * Check if title ends with a pattern (case-insensitive)
     */
    public boolean titleEndsWith(String pattern) {
        return titleString.toLowerCase().endsWith(pattern.toLowerCase());
    }
}
