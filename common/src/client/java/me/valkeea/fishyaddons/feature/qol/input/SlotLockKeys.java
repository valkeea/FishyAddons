package me.valkeea.fishyaddons.feature.qol.input;

import me.valkeea.fishyaddons.feature.item.safeguard.FGUtil;
import me.valkeea.fishyaddons.feature.item.safeguard.SlotHandler;
import me.valkeea.fishyaddons.mixin.AbstractContainerScreenAccessor;
import me.valkeea.fishyaddons.util.ContainerScanner;
import me.valkeea.fishyaddons.util.Keyboard;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import me.valkeea.fishyaddons.vconfig.config.impl.ItemConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;

@VCModule
public final class SlotLockKeys {
    private SlotLockKeys() {}

    private static final SlotGesture<AbstractContainerScreen<?>, Slot> gesture = new SlotGesture<>();
    private static boolean changed;
    private static int keyCode;

    @VCListener(strings = StringKey.KEY_LOCK_SLOT)
    static void onKeyChange(String v) {
        changed = keyCode != 0;
        keyCode = "NONE".equals(v) ? -1 : Keyboard.getKeyCodeFromString(v);
    }

    static void update(Minecraft client, AbstractContainerScreen<?> screen) {
        if (keyCode == -1) {
            gesture.cancel(false);
            return;
        }

        var pressed = Keyboard.isKeyDown(keyCode);

        if (changed) {
            gesture.cancel(pressed);
            changed = false;
        }

        var hovered = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();
        var allowed = hovered != null && hovered.container == client.player.getInventory();
        var selection = gesture.update(screen, hovered, allowed, pressed);
        if (selection == null || !ContainerScanner.isGuiOrInv()) return;

        int start = SlotHandler.remap(screen, selection.start().index);
        if (!validSlot(screen, start)) return;
        
        if (selection.start() == selection.end()) {
            keyTap(start);
            return;
        }

        int end = SlotHandler.remap(screen, selection.end().index);
        if (validSlot(screen, end)) SlotHandler.bindSlots(start, end);
    }

    static void cancel() {
        gesture.cancel(true);
    }

    static void suppressHeldKey() {
        gesture.cancel(keyCode != -1 && Keyboard.isKeyDown(keyCode));
    }

    private static void keyTap(int slotId) {
        if (FGUtil.isSlotLocked(slotId)) SlotHandler.unlockSlot(slotId);
        else if (FGUtil.isSlotBound(slotId)) SlotHandler.unbindSlots(slotId, ItemConfig.getBoundSlot(slotId));
        else SlotHandler.lockSlot(slotId);
    }

    private static boolean validSlot(AbstractContainerScreen<?> s, int i) {
        if (s instanceof InventoryScreen) return i >= 5 && i <= 43;
        return s instanceof ContainerScreen && i >= 9 && i <= 43;
    }
}
