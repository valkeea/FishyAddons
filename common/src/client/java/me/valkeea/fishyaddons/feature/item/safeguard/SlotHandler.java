package me.valkeea.fishyaddons.feature.item.safeguard;

import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.util.ContainerScanner;
import me.valkeea.fishyaddons.vconfig.config.impl.ItemConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class SlotHandler {
    private SlotHandler() {}

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
        if (!FGUtil.isKeyBound()) return false;
        int index = hovered.id;
        int invIndex = remap(screen, index);
        if (invIndex == -1) return false;
        return FGUtil.isSlotLocked(invIndex);
    }

    private static boolean isBoundClick(HandledScreen<?> screen, Slot hovered, int index, int invIndex) {
        if (isInvalidContext(screen, invIndex)) return false;
        if (!isShiftDown()) return true;

        int boundSlotId = ItemConfig.getBoundSlot(invIndex);
        var handler = screen.getScreenHandler();
        if (handler == null || remapInventory(boundSlotId) == -1) {
            return false;
        }

        var boundSlot = handler.getSlot(boundSlotId);
        var hoveredStack = hovered.getStack();
        var boundStack = boundSlot.getStack();

        if (!canInsertItems(hovered, boundSlot, hoveredStack, boundStack)) {
            return true;
        }

        return swapOrMoveItems(
            MinecraftClient.getInstance(),
            handler, invIndex, boundSlotId,
            index, hoveredStack, boundStack
        );
    }

    private static boolean isInvalidContext(HandledScreen<?> screen, int invIndex) {
        return !FGUtil.isKeyBound() || !FGUtil.isSlotBound(invIndex) || !ContainerScanner.isGuiOrInv() ||
        !(screen instanceof InventoryScreen);
    }

    private static boolean isShiftDown() {
        var mc = MinecraftClient.getInstance();
        if (mc.options == null) return false;
        int keyCode = mc.options.sneakKey.getDefaultKey().getCode();
        return InputUtil.isKeyPressed(mc.getWindow(), keyCode);
    } 

    private static boolean canInsertItems(Slot hovered, Slot boundSlot, ItemStack hoveredStack, ItemStack boundStack) {
        return (hoveredStack.isEmpty() || boundSlot.canInsert(hoveredStack))
            && (boundStack.isEmpty() || hovered.canInsert(boundStack));
    }

    private static boolean swapOrMoveItems(MinecraftClient mc, ScreenHandler handler, int invIndex, int boundSlotId, int index, ItemStack hoveredStack, ItemStack boundStack) {

        boolean hasHoveredStack = !hoveredStack.isEmpty();
        boolean hasBoundStack = !boundStack.isEmpty();
        if (!hasHoveredStack && !hasBoundStack) {
            return false;
        }

        int syncId = handler.syncId;
        var interactionManager = mc.interactionManager;
        var player = mc.player;

        if (hasHoveredStack) {
            interactionManager.clickSlot(syncId, invIndex, 0, SlotActionType.PICKUP, player);
            interactionManager.clickSlot(syncId, boundSlotId, 0, SlotActionType.PICKUP, player);
            if (hasBoundStack) {
                interactionManager.clickSlot(syncId, invIndex, 0, SlotActionType.PICKUP, player);
            }
        } else {
            interactionManager.clickSlot(syncId, boundSlotId, 0, SlotActionType.PICKUP, player);
            interactionManager.clickSlot(syncId, index, 0, SlotActionType.PICKUP, player);
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
    public static int remap(HandledScreen<?> screen, int slotId) {
        ScreenHandler handler = screen.getScreenHandler();
        if (handler == null) return -1;
        
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
