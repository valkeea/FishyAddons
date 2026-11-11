package me.valkeea.fishyaddons.feature.item.safeguard;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import me.valkeea.fishyaddons.mixin.AbstractContainerScreenAccessor;
import me.valkeea.fishyaddons.mixin.PlayerInventoryAccessor;
import me.valkeea.fishyaddons.util.ZoneUtils;
import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.ContainerKeyInputEvent;
import me.valkeea.fishyaddons.event.impl.FaEvents;

public final class DropProtection {
    private DropProtection() {}
    private static final int HOTBAR_OFFSET = 36;

    public static void init() {
        FaEvents.CONTAINER_KEY_INPUT.register(DropProtection::onKey, EventPriority.HIGHEST);
    }

    static void onKey(ContainerKeyInputEvent e) {
        var slot = e.hoveredSlot;
        if (slot == null || !slot.hasItem()) return;
        if (!e.mc.options.keyDrop.matches(e.input)) return;

        int index = SlotHandler.remap(e.screen, slot.index);
        if (index != -1 && FGUtil.preventSlotClick(index)) {
            e.consumeWithTrue();
            return;
        }

        if (!FGUtil.isProtected(slot.getItem())) return;
        FGUtil.triggerProtection();
        e.consumeWithTrue();
    }

    public static boolean blockThrow(LocalPlayer player) {
        if (ZoneUtils.isInDungeon() && !McApi.screenIsActive()) return false;

        int selectedSlot = ((PlayerInventoryAccessor) player.getInventory()).getSelectedSlot();
        int index = HOTBAR_OFFSET + selectedSlot;
        if (index != -1 && FGUtil.preventSlotClick(index)) return true;

        var stack = player.getMainHandItem();
        if (FGUtil.isProtected(stack)) {
            FGUtil.triggerProtection();
            return true;
        }

        return false;
    }

    public static boolean blockDrag(MouseButtonEvent click, AbstractContainerScreen<?> screen, ItemStack stack) {
        var accessor = (AbstractContainerScreenAccessor) screen;
        int w = (screen.width - accessor.getImageWidth()) / 2;
        int h = (screen.height - accessor.getImageHeight()) / 2;

        boolean outsideGui = click.x() < w || click.x() > w + accessor.getImageWidth() ||
        click.y() < h || click.y() > h + accessor.getImageHeight();

        boolean notHovering = accessor.getHoveredSlot() == null;
        if (GuiHandler.isProtectedCached(stack) &&
            outsideGui && notHovering) {
            FGUtil.triggerProtection();
            return true;
        }
        return false;
    }    
}
