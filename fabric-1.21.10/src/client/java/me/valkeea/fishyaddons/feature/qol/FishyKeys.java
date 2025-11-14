package me.valkeea.fishyaddons.feature.qol;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.feature.item.safeguard.SlotHandler;
import me.valkeea.fishyaddons.mixin.HandledScreenAccessor;
import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.util.SbGui;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;

public class FishyKeys {
    private FishyKeys() {}

    private static boolean dragging = false;
    private static Slot bindStart = null;
    private static boolean wasPressed = false;
    private static long lastGuiSlotAddTime = 0;
    private static final long GUI_SLOT_ADD_COOLDOWN_MS = 200;
    
    private static boolean wasChatOpen = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean isChatOpen = client.currentScreen instanceof ChatScreen;

            if (client.currentScreen != null) {
                if (client.currentScreen instanceof HandledScreen<?> screen) {
                    ifInventory(client, screen);
                } else if (isChatOpen) {
                    resetLockKeyState();
                } else {
                    resetLockKeyState();
                }
                
            } else {
                if (wasChatOpen) KeyShortcut.notifyChatClosed();
                KeyShortcut.handleShortcuts();
            }

            wasChatOpen = isChatOpen;
        });
    }

    private static void ifInventory(MinecraftClient client, HandledScreen<?> screen) {
        slotLocking(client, screen);
        configureIcons(client, screen);
    }

    private static void configureIcons(MinecraftClient client, HandledScreen<?> screen) {
        String guiKey = FishyConfig.getKeyString(Key.MOD_KEY_LOCK_GUISLOT);

        if (guiKey == null || "NONE".equals(guiKey)) {
            return;
        }

        int guiKeyCode = Keyboard.getKeyCodeFromString(guiKey);

        if (guiKeyCode == -1) {
            return;
        }

        boolean isPressed = InputUtil.isKeyPressed(
            MinecraftClient.getInstance().getWindow(), 
            guiKeyCode
        );
        
        Slot hovered = ((HandledScreenAccessor) screen).getFocusedSlot();
        String title = screen.getTitle().getString();

        long now = System.currentTimeMillis();
        if (isPressed && hovered != null && hovered.inventory != client.player.getInventory()) {
            if (now - lastGuiSlotAddTime > GUI_SLOT_ADD_COOLDOWN_MS) {
                me.valkeea.fishyaddons.feature.skyblock.GuiIcons.addGuiSlot(title, hovered.id);
                lastGuiSlotAddTime = now;
            }
        } else if (!isPressed) {
            lastGuiSlotAddTime = 0;
        }
    }

    private static void slotLocking(MinecraftClient client, HandledScreen<?> screen) {
        String lockKey = FishyConfig.getKeyString(Key.MOD_KEY_LOCK);

        if (lockKey == null || "NONE".equals(lockKey)) {
            resetLockKeyState();
            return;
        }

        int lockKeyCode = Keyboard.getKeyCodeFromString(lockKey);
        
        if (lockKeyCode == -1) {
            resetLockKeyState();
            return;
        }

        boolean isPressed = InputUtil.isKeyPressed(
            MinecraftClient.getInstance().getWindow(), 
            lockKeyCode
        );
        
        Slot hovered = ((HandledScreenAccessor) screen).getFocusedSlot();

        lockKeyPress(client, hovered, isPressed);
        lockKeyRelease(screen, hovered, isPressed);
        
        wasPressed = isPressed;
    }

    private static void lockKeyPress(MinecraftClient client, Slot hovered, boolean isPressed) {
        if (isPressed && !wasPressed && hovered != null && hovered.inventory == client.player.getInventory()) {
            bindStart = hovered;
            dragging = true;
        }
    }

    private static void lockKeyRelease(HandledScreen<?> screen, Slot hovered, boolean isPressed) {
        if (!isPressed && wasPressed && dragging && 
            bindStart != null && SbGui.isPlayerInventory() && hovered != null) {
            if (hovered == bindStart) {
                singleSlotAction(screen, hovered);
            } else {
                slotBinding(screen, hovered);
            }
            resetDragState();
        }
    }

    private static void singleSlotAction(HandledScreen<?> screen, Slot hovered) {
        int slotId = SlotHandler.remap(screen, hovered.id);
        if (!isValidSlot(slotId)) return;

        if (SlotHandler.isSlotLocked(slotId)) {
            SlotHandler.unlockSlot(slotId);
        } else if (SlotHandler.isSlotBound(slotId)) {
            int other = SlotHandler.getBoundSlot(slotId);
            SlotHandler.unbindSlots(slotId, other);
        } else {
            SlotHandler.lockSlot(slotId);
        }
    }

    private static void slotBinding(HandledScreen<?> screen, Slot hovered) {
        int startId = SlotHandler.remap(screen, bindStart.id);
        int endId = SlotHandler.remap(screen, hovered.id);
        
        if (isValidSlot(startId) && isValidSlot(endId)) {
            SlotHandler.bindSlots(startId, endId);
        }
    }

    private static void resetLockKeyState() {
        resetDragState();
        wasPressed = false;
    }

    private static void resetDragState() {
        dragging = false;
        bindStart = null;
    }

    private static boolean isValidSlot(int remapId) {
        HandledScreen<?> screen = MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?> ? 
            (HandledScreen<?>) MinecraftClient.getInstance().currentScreen : null;
        return (screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen && 
            remapId >= 5 && remapId <= 43) ||
            (screen instanceof net.minecraft.client.gui.screen.ingame.GenericContainerScreen && 
            remapId >= 9 && remapId <= 43);
    }
}
