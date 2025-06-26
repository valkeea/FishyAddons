package me.valkeea.fishyaddons.event;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.handler.KeyShortcut;
import me.valkeea.fishyaddons.mixin.HandledScreenAccessor;
import me.valkeea.fishyaddons.util.KeyUtil;
import me.valkeea.fishyaddons.util.HelpUtil;
import me.valkeea.fishyaddons.safeguard.SlotProtectionManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;

public class FishyKeys {
    private FishyKeys() {}

    private static boolean dragging = false;
    private static Slot bindStart = null;
    private static boolean wasLockKeyPressed = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean isScreenOpen = client.currentScreen != null;
            int lockKeyCode = KeyUtil.getKeyCodeFromString(FishyConfig.getLockKey());
            boolean isLockKeyPressed = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), lockKeyCode);

            if (client.currentScreen instanceof HandledScreen<?> screen) {
                Slot hovered = ((HandledScreenAccessor) screen).getFocusedSlot();

                // Just pressed
                if (isLockKeyPressed && !wasLockKeyPressed) {
                    if (hovered != null && hovered.inventory == client.player.getInventory()) {
                        bindStart = hovered;
                        dragging = true;
                    }
                }

                // Just released
                if (!isLockKeyPressed && wasLockKeyPressed && dragging && 
                    bindStart != null && HelpUtil.isPlayerInventory()) {
                    if (hovered != null) {
                        if (hovered == bindStart) {
                            int slotId = SlotProtectionManager.remap(screen, hovered.id);
                            if (slotId >= 5 && slotId < 45) {
                                if (SlotProtectionManager.isSlotLocked(slotId)) {
                                    SlotProtectionManager.unlockSlot(slotId);
                                } else if (SlotProtectionManager.isSlotBound(slotId)) {
                                    int other = SlotProtectionManager.getBoundSlot(slotId);
                                    SlotProtectionManager.unbindSlots(slotId, other);
                                } else {
                                    SlotProtectionManager.lockSlot(slotId);
                                }
                            }
                        } else {
                            int startId = SlotProtectionManager.remap(screen, bindStart.id);
                            int endId = SlotProtectionManager.remap(screen, hovered.id);
                            if (startId >= 5 && startId < 45 && endId >= 5 && endId < 45) {
                                SlotProtectionManager.bindSlots(startId, endId);
                            }
                        }
                    }
                    dragging = false;
                    bindStart = null;
                }
                wasLockKeyPressed = isLockKeyPressed;
            } else {
                // Not in a HandledScreen, reset drag state and key state
                dragging = false;
                bindStart = null;
                wasLockKeyPressed = false;
            }

            // --- All other keybinds only work when no screen is open ---
            if (!isScreenOpen) {
                KeyShortcut.handleShortcuts();
            }
        });
    }
}