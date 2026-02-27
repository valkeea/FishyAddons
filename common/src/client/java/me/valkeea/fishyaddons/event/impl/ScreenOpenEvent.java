package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

/**
 * Event triggered when a new container or inventory is opened,
 * or when the title of an already open container is updated
 */
public class ScreenOpenEvent extends BaseEvent {
    public final GenericContainerScreen screen;
    public final Text title;
    public final String titleString;

    public ScreenOpenEvent(GenericContainerScreen screen, Text title) {

        this.screen = screen;
        this.title = title;
        this.titleString = title == null ? "" : title.getString();
    }

    /** Check if title matches a pattern (case-insensitive) */
    public boolean titleContains(String pattern) {
        return titleString.toLowerCase().contains(pattern.toLowerCase());
    }    

    /** Get slot at specific index */
    public Slot getSlot(int index) {
        if (screen == null || screen.getScreenHandler() == null) {
            return null;
        }
        var slots = screen.getScreenHandler().slots;
        if (index >= 0 && index < slots.size()) {
            return slots.get(index);
        }
        return null;
    }

    /** Get item stack at specific slot index */
    public ItemStack getStackAt(int index) {
        Slot slot = getSlot(index);
        return slot != null ? slot.getStack() : ItemStack.EMPTY;
    }    
}
