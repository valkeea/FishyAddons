package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Event triggered when a new container or inventory is opened,
 * or when the title of an already open container is updated
 */
public class ScreenOpenEvent extends BaseEvent {
    public final ContainerScreen screen;
    public final Component title;
    public final String titleString;

    public ScreenOpenEvent(ContainerScreen screen, Component title) {

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
        if (screen == null) return null;
        var slots = screen.getMenu().slots;
        if (index >= 0 && index < slots.size()) {
            return slots.get(index);
        }
        return null;
    }

    /** Get item stack at specific slot index */
    public ItemStack getStackAt(int index) {
        var slot = getSlot(index);
        return slot != null ? slot.getItem() : ItemStack.EMPTY;
    }    
}
