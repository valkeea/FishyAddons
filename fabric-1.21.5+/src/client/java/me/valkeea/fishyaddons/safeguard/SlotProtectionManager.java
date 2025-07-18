package me.valkeea.fishyaddons.safeguard;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.util.PlaySound;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public class SlotProtectionManager {
    private SlotProtectionManager() {}

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
