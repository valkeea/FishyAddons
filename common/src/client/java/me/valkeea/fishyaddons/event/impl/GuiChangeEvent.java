package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.text.Text;

/**
 * Event triggered when the contents of a container change, or one is opened
 * */
public class GuiChangeEvent extends BaseEvent {
    public final GenericContainerScreen screen;
    public final Text title;
    public final String titleString;

    public GuiChangeEvent(GenericContainerScreen screen, Text title) {
        this.screen = screen;
        this.title = title;
        this.titleString = title == null ? "" : title.getString();
    }

    /** Check if title matches a pattern (case-insensitive) */
    public boolean titleContains(String pattern) {
        return titleString.toLowerCase().contains(pattern.toLowerCase());
    }

    /** Check if title ends with a pattern (case-insensitive) */
    public boolean titleEndsWith(String pattern) {
        return titleString.toLowerCase().endsWith(pattern.toLowerCase());
    }
}
