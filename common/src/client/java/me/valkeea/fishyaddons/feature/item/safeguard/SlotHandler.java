package me.valkeea.fishyaddons.feature.item.safeguard;

import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.util.ContainerScanner;
import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.vconfig.config.impl.ItemConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotHandler {
    private SlotHandler() {}

    public static void init() {
        FaEvents.SCREEN_MOUSE_CLICK.register(e -> {
            if (e.hoveredSlot != null &&
                (isLockedClick(e.screen, e.hoveredSlot) ||
                 isBoundClick(e.screen, e.hoveredSlot, e.hoveredSlot.index, remap(e.screen, e.hoveredSlot.index)))) {
                e.setConsumed(true);
            }
        }, EventPriority.HIGHEST, EventPhase.PRE);
    }

    private static boolean isLockedClick(AbstractContainerScreen<?> screen, Slot hovered) {
        if (!FGUtil.isKeyBound()) return false;
        int index = hovered.index;
        int invIndex = remap(screen, index);
        if (invIndex == -1) return false;
        return FGUtil.isSlotLocked(invIndex) || lockedBoundClick(screen, invIndex);
    }

    private static boolean lockedBoundClick(AbstractContainerScreen<?> screen, int invIndex) {
        if (!FGUtil.isSlotBound(invIndex)) return false;
        boolean inInv = screen instanceof InventoryScreen;
        return (!Keyboard.isShiftDown() && inInv) || !inInv;
    }

    private static boolean isBoundClick(AbstractContainerScreen<?> screen, Slot hovered, int index, int invIndex) {
        if (isInvalidContext(screen, invIndex)) return false;
        if (!Keyboard.isShiftDown()) return true;

        int boundSlotId = ItemConfig.getBoundSlot(invIndex);
        var handler = screen.getMenu();
        if (remapInventory(boundSlotId) == -1) {
            return false;
        }

        var boundSlot = handler.getSlot(boundSlotId);
        var hoveredStack = hovered.getItem();
        var boundStack = boundSlot.getItem();
        if (!canInsertItems(hovered, boundSlot, hoveredStack, boundStack)) {
            return true;
        }

        return swapOrMoveItems(
            Minecraft.getInstance(),
            handler, invIndex, boundSlotId,
            index, hoveredStack, boundStack
        );
    }

    private static boolean isInvalidContext(AbstractContainerScreen<?> screen, int invIndex) {
        return !FGUtil.isKeyBound() || !FGUtil.isSlotBound(invIndex) || !ContainerScanner.isGuiOrInv() ||
        !(screen instanceof InventoryScreen);
    }

    private static boolean canInsertItems(Slot hovered, Slot boundSlot, ItemStack hoveredStack, ItemStack boundStack) {
        return (hoveredStack.isEmpty() || boundSlot.mayPlace(hoveredStack))
            && (boundStack.isEmpty() || hovered.mayPlace(boundStack));
    }

    private static boolean swapOrMoveItems(Minecraft mc, AbstractContainerMenu handler, int invIndex, int boundSlotId, int index, ItemStack hoveredStack, ItemStack boundStack) {

        boolean hasHoveredStack = !hoveredStack.isEmpty();
        boolean hasBoundStack = !boundStack.isEmpty();
        if (!hasHoveredStack && !hasBoundStack) {
            return false;
        }

        int syncId = handler.containerId;
        var interactionManager = mc.gameMode;
        var player = mc.player;

        if (hasHoveredStack) {
            interactionManager.handleContainerInput(syncId, invIndex, 0, ContainerInput.PICKUP, player);
            interactionManager.handleContainerInput(syncId, boundSlotId, 0, ContainerInput.PICKUP, player);
            if (hasBoundStack) {
                interactionManager.handleContainerInput(syncId, invIndex, 0, ContainerInput.PICKUP, player);
            }

        } else {
            interactionManager.handleContainerInput(syncId, boundSlotId, 0, ContainerInput.PICKUP, player);
            interactionManager.handleContainerInput(syncId, index, 0, ContainerInput.PICKUP, player);
        }

        return true;
    }

    // --- Lock/Unlock ---

    public static void lockSlot(int slot) {
        if (!FGUtil.isSlotLocked(slot)) {
            ItemConfig.toggleSlotLock(slot);
            PlaySound.playBindOrLock();
        }
    }

    public static void unlockSlot(int slot) {
        if (FGUtil.isSlotLocked(slot)) {
            ItemConfig.toggleSlotLock(slot);
            PlaySound.playUnbindOrUnlock();
        }
    }

    // --- Bind/Unbind ---

    public static void bindSlots(int slotA, int slotB) {
        if (!FGUtil.isSlotBound(slotA) && !FGUtil.isSlotBound(slotB)) {
            ItemConfig.bindSlots(slotA, slotB);
            PlaySound.playBindOrLock();
        }
    }

    public static void unbindSlots(int slotA, int slotB) {
        if (FGUtil.isSlotBound(slotA) && FGUtil.isSlotBound(slotB) && ItemConfig.getBoundSlot(slotA) == slotB) {
            ItemConfig.unbindSlots(slotA, slotB);
            PlaySound.playUnbindOrUnlock();
        }
    }

    /**
     * Remap screen slot ids to normalize player inventory
     * (5-8 armor, 9-35 main inventory, 36-43 hotbar, 44 / 9th hotbar slot is invalid).
     * 
     * @return Remapped slot id, or -1 if invalid
     */
    public static int remap(AbstractContainerScreen<?> screen, int slotId) {
        var handler = screen.getMenu();
        int totalSlots = handler.slots.size();
        
        return screen instanceof InventoryScreen 
            ? remapInventory(slotId)
            : remapContainer(slotId, totalSlots);
    }

    /** Inventory is 1:1, slots 5-43 belong to the accessible player section */
    private static int remapInventory(int slotId) {
        return (slotId >= 5 && slotId <= 43)
            ? slotId
            : -1;
    }

    /** From the last 46 slots: 5-8 armor, 9-35 main inventory, 36-43 accessible hotbar */
    private static int remapContainer(int slotId, int totalSlots) {
        int playerStart = totalSlots - 36;
        if (slotId < playerStart || slotId >= totalSlots) return -1;
        
        int relativeSlot = slotId - playerStart;
        
        if (relativeSlot < 27) {
            return 9 + relativeSlot;

        } else {
            int hotbarIndex = relativeSlot - 27;
            return hotbarIndex >= 8 ? -1 : 36 + hotbarIndex;
        }
    }
}
