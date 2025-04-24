package me.wait.fishyaddons.util;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

// Abstract base class to mimic GuiContainerEvent
public abstract class GuiClick extends Event {
    protected final GuiContainer gui;
    protected final Container container;

    protected GuiClick(GuiContainer gui, Container container) {
        this.gui = gui;
        this.container = container;
    }

    public GuiContainer getGui() {
        return gui;
    }

    public Container getContainer() {
        return container;
    }

    @Cancelable
    public static class SlotClickEvent extends GuiClick {
        private final Slot slot;
        private final int slotId;
        private final int clickedButton;
        private final int clickType;

        public SlotClickEvent(GuiContainer gui, Container container, Slot slot, int slotId, int clickedButton, int clickType) {
            super(gui, container);
            this.slot = slot;
            this.slotId = slotId;
            this.clickedButton = clickedButton;
            this.clickType = clickType;
        }

        public Slot getSlot() {
            return slot;
        }

        public int getSlotId() {
            return slotId;
        }

        public int getClickedButton() {
            return clickedButton;
        }

        public int getClickType() {
            return clickType;
        }
    }
}