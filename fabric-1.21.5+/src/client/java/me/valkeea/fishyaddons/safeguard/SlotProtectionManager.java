package me.valkeea.fishyaddons.safeguard;

import org.lwjgl.glfw.GLFW;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.util.SbGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class SlotProtectionManager {
    private SlotProtectionManager() {}

    public static void init() {
        FaEvents.SCREEN_MOUSE_CLICK.register(event -> {
            if (event.hoveredSlot != null &&
                (isLockedClick(event.screen, event.hoveredSlot) ||
                 isBoundClick(event.screen, event.hoveredSlot, event.hoveredSlot.id, remap(event.screen, event.hoveredSlot.id)))) {
                event.setConsumed(true);
            }
        }, EventPriority.HIGHEST, EventPhase.PRE);
    }

    private static boolean isLockedClick(HandledScreen<?> screen, Slot hovered) {
        int index = hovered.id;
        int invIndex = SlotProtectionManager.remap(screen, index);
        if (invIndex <= 8 || invIndex >= 44) return false;
        return isSlotLocked(invIndex);
    }

    private static boolean isBoundClick(HandledScreen<?> screen, Slot hovered, int index, int invIndex) {
        if (!SlotProtectionManager.isSlotBound(invIndex) || !SbGui.isPlayerInventory()) {
            return false;
        }

        if (!isShiftDown()) {
            return true;
        }

        int boundSlotId = getBoundSlot(invIndex);
        ScreenHandler handler = screen.getScreenHandler();
        if (handler == null || boundSlotId < 0 || boundSlotId >= handler.slots.size()) {
            return false;
        }
        Slot boundSlot = handler.getSlot(boundSlotId);

        ItemStack hoveredStack = hovered.getStack();
        ItemStack boundStack = boundSlot.getStack();

        if (!canInsertItems(hovered, boundSlot, hoveredStack, boundStack)) {
            return true;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        return swapOrMoveItems(client, handler, invIndex, boundSlotId, index, hoveredStack, boundStack);
    }

    private static boolean isShiftDown() {
        MinecraftClient client = MinecraftClient.getInstance();
        long handle = client.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static boolean canInsertItems(Slot hovered, Slot boundSlot, ItemStack hoveredStack, ItemStack boundStack) {
        return (hoveredStack.isEmpty() || boundSlot.canInsert(hoveredStack))
            && (boundStack.isEmpty() || hovered.canInsert(boundStack));
    }

    private static boolean swapOrMoveItems(MinecraftClient client, ScreenHandler handler, int invIndex, int boundSlotId, int index, ItemStack hoveredStack, ItemStack boundStack) {

        if (!hoveredStack.isEmpty() && !boundStack.isEmpty()) {
            client.interactionManager.clickSlot(handler.syncId, invIndex, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(handler.syncId, boundSlotId, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(handler.syncId, invIndex, 0, SlotActionType.PICKUP, client.player);
            return true;
        }

        if (!hoveredStack.isEmpty() && boundStack.isEmpty()) {
            client.interactionManager.clickSlot(handler.syncId, invIndex, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(handler.syncId, boundSlotId, 0, SlotActionType.PICKUP, client.player);
            return true;
        }

        if (hoveredStack.isEmpty() && !boundStack.isEmpty()) {
            client.interactionManager.clickSlot(handler.syncId, boundSlotId, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(handler.syncId, index, 0, SlotActionType.PICKUP, client.player);
            return true;
        }
        
        return false;
    }    

    public static boolean isSlotLocked(int slot) {
        return ItemConfig.isSlotLocked(slot);
    }

    public static boolean isSlotBound(int slot) {
        return ItemConfig.getBoundSlot(slot) != -1;
    }

    public static int getBoundSlot(int slot) {
        return ItemConfig.getBoundSlot(slot);
    }

    // --- Lock/Unlock ---

    public static void lockSlot(int slot) {
        if (!isSlotLocked(slot)) {
            ItemConfig.toggleSlotLock(slot);
            PlaySound.playBindOrLock();
        }
    }

    public static void unlockSlot(int slot) {
        if (isSlotLocked(slot)) {
            ItemConfig.toggleSlotLock(slot);
            PlaySound.playUnbindOrUnlock();
        }
    }

    // --- Bind/Unbind ---

    public static void bindSlots(int slotA, int slotB) {
        if (!isSlotBound(slotA) && !isSlotBound(slotB)) {
            ItemConfig.bindSlots(slotA, slotB);
            PlaySound.playBindOrLock();
        }
    }

    public static void unbindSlots(int slotA, int slotB) {
        if (isSlotBound(slotA) && isSlotBound(slotB) && getBoundSlot(slotA) == slotB) {
            ItemConfig.unbindSlots(slotA, slotB);
            PlaySound.playUnbindOrUnlock();
        }
    }

    public static int remap(HandledScreen<?> screen, int slotId) {
        String guiClass = screen.getClass().getSimpleName();
        if (guiClass.equals("class_490") && slotId >= 5 && slotId <= 43) {
            // Player inventory screen
            return slotId;
        } else if (guiClass.equals("class_476")) {
            int s = slotId - 45;
            if (s >= 9 && s <= 43) {
                return s;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    public static int indexToSlotId(HandledScreen<?> screen, int playerInvIndex) {
        String guiClass = screen.getClass().getSimpleName();
        if (!guiClass.equals("class_475")) return -1;
        return playerInvIndex + 45; // Convert back to server slot index
    }
}
