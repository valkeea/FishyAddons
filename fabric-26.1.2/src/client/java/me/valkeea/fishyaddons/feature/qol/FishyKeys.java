package me.valkeea.fishyaddons.feature.qol;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.feature.item.safeguard.FGUtil;
import me.valkeea.fishyaddons.feature.item.safeguard.SlotHandler;
import me.valkeea.fishyaddons.mixin.HandledScreenAccessor;
import me.valkeea.fishyaddons.util.ContainerScanner;
import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import me.valkeea.fishyaddons.vconfig.config.impl.ItemConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;

public class FishyKeys {
    private FishyKeys() {}

    private static boolean wasChatOpen = false;
    private static boolean dragging = false;
    private static Slot bindStart = null;
    private static boolean wasPressed = false;
    private static long lastGuiSlotAddTime = 0;
    private static final long GUI_SLOT_ADD_COOLDOWN_MS = 200;

    public static void register() {

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null) return;

            var screen = mc.screen;
            boolean chatOpen = screen instanceof ChatScreen;
            if (screen != null) {

                if (screen instanceof AbstractContainerScreen<?> acs) {
                    ifInventory(mc, acs);
                } else {
                    resetLockKeyState();
                }
                
            } else {
                if (wasChatOpen) KeyShortcut.notifyChatClosed();
                KeyShortcut.handleShortcuts();
            }

            wasChatOpen = chatOpen;
        });
    }

    private static void ifInventory(Minecraft client, AbstractContainerScreen<?> screen) {
        slotLocking(client, screen);
        configureIcons(client, screen);
    }

    private static void configureIcons(Minecraft client, AbstractContainerScreen<?> screen) {

        var guiKey = Config.get(StringKey.KEY_HIDE_GUI);
        if ("NONE".equals(guiKey)) return;

        int guiKeyCode = Keyboard.getKeyCodeFromString(guiKey);
        if (guiKeyCode == -1) return;

        var isPressed = InputConstants.isKeyDown(
            Minecraft.getInstance().getWindow(), 
            guiKeyCode
        );
        
        var hovered = ((HandledScreenAccessor) screen).getHoveredSlot();
        var title = screen.getTitle().getString();

        long now = System.currentTimeMillis();
        if (isPressed && hovered != null && hovered.container != client.player.getInventory()) {
            if (now - lastGuiSlotAddTime > GUI_SLOT_ADD_COOLDOWN_MS) {
                me.valkeea.fishyaddons.feature.skyblock.GuiIcons.addGuiSlot(title, hovered.index);
                lastGuiSlotAddTime = now;
            }

        } else if (!isPressed) {
            lastGuiSlotAddTime = 0;
        }
    }

    private static void slotLocking(Minecraft client, AbstractContainerScreen<?> screen) {

        var lockKey = Config.get(StringKey.KEY_LOCK_SLOT);
        if ("NONE".equals(lockKey)) {
            resetLockKeyState();
            return;
        }

        int lockKeyCode = Keyboard.getKeyCodeFromString(lockKey);
        if (lockKeyCode == -1) {
            resetLockKeyState();
            return;
        }

        var isPressed = InputConstants.isKeyDown(
            Minecraft.getInstance().getWindow(), 
            lockKeyCode
        );
        
        var hovered = ((HandledScreenAccessor) screen).getHoveredSlot();

        lockKeyPress(client, hovered, isPressed);
        lockKeyRelease(screen, hovered, isPressed);
        
        wasPressed = isPressed;
    }

    private static void lockKeyPress(Minecraft client, Slot hovered, boolean isPressed) {
        if (isPressed && !wasPressed && hovered != null && hovered.container == client.player.getInventory()) {
            bindStart = hovered;
            dragging = true;
        }
    }

    private static void lockKeyRelease(AbstractContainerScreen<?> screen, Slot hovered, boolean isPressed) {

        if (!isPressed && wasPressed && dragging && 
            bindStart != null && ContainerScanner.isGuiOrInv() && hovered != null) {

            if (hovered == bindStart) {
                singleSlotAction(screen, hovered);
            } else {
                slotBinding(screen, hovered);
            }

            resetDragState();
        }
    }

    private static void singleSlotAction(AbstractContainerScreen<?> screen, Slot hovered) {

        int slotId = SlotHandler.remap(screen, hovered.index);
        if (!isValidSlot(screen, slotId)) return;

        if (FGUtil.isSlotLocked(slotId)) {
            SlotHandler.unlockSlot(slotId);

        } else if (FGUtil.isSlotBound(slotId)) {
            int other = ItemConfig.getBoundSlot(slotId);
            SlotHandler.unbindSlots(slotId, other);

        } else {
            SlotHandler.lockSlot(slotId);
        }
    }

    private static void slotBinding(AbstractContainerScreen<?> screen, Slot hovered) {
        int startId = SlotHandler.remap(screen, bindStart.index);
        int endId = SlotHandler.remap(screen, hovered.index);
        if (isValidSlot(screen, startId) && isValidSlot(screen, endId)) SlotHandler.bindSlots(startId, endId);
    }

    private static void resetLockKeyState() {
        resetDragState();
        wasPressed = false;
    }

    private static void resetDragState() {
        dragging = false;
        bindStart = null;
    }

    private static boolean isValidSlot(AbstractContainerScreen<?> s, int id) {
        boolean inventory = s instanceof InventoryScreen;
        boolean container = s instanceof ContainerScreen;

        if (inventory) return id >= 5 && id <= 43;
        return container && id >= 9 && id <= 43;
    }
}
