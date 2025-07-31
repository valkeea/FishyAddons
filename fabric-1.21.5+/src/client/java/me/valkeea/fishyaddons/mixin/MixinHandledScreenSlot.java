package me.valkeea.fishyaddons.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.safeguard.SlotProtectionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@Mixin(HandledScreen.class)
public class MixinHandledScreenSlot {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        Slot hovered = ((HandledScreenAccessor) screen).getFocusedSlot();
        if (hovered == null) return;
        int index = hovered.id;
        int invIndex = SlotProtectionManager.remap(screen, index );
        if (invIndex <= 8 || invIndex >= 43) return;

        if (handleLockedSlot(invIndex, cir)) return;
        handleBoundSlot(screen, hovered, index, invIndex, cir);
    }

    private boolean handleLockedSlot(int invIndex, CallbackInfoReturnable<Boolean> cir) {
        if (SlotProtectionManager.isSlotLocked(invIndex)) {
            cir.setReturnValue(false);
            return true;
        }
        return false;
    }

    private boolean handleBoundSlot(HandledScreen<?> screen, Slot hovered, int index, int invIndex, CallbackInfoReturnable<Boolean> cir) {
        if (!SlotProtectionManager.isSlotBound(invIndex)) {
            return false;
        }

        if (!isShiftDown()) {
            cir.setReturnValue(false);
            return true;
        }

        if (!(screen instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) {
            cir.setReturnValue(false);
            return true;
        }

        int boundSlotId = SlotProtectionManager.getBoundSlot(invIndex);
        ScreenHandler handler = screen.getScreenHandler();
        if (handler == null || boundSlotId < 0 || boundSlotId >= handler.slots.size()) {
            cir.setReturnValue(false);
            return true;
        }
        Slot boundSlot = handler.getSlot(boundSlotId);

        ItemStack hoveredStack = hovered.getStack();
        ItemStack boundStack = boundSlot.getStack();

        if (!canInsertItems(hovered, boundSlot, hoveredStack, boundStack, cir)) {
            return true;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (swapOrMoveItems(client, handler, invIndex, boundSlotId, index, hoveredStack, boundStack, cir)) {
            return true;
        }

        cir.setReturnValue(false);
        return true;
    }

    private boolean isShiftDown() {
        MinecraftClient client = MinecraftClient.getInstance();
        long handle = client.getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private boolean canInsertItems(Slot hovered, Slot boundSlot, ItemStack hoveredStack, ItemStack boundStack, CallbackInfoReturnable<Boolean> cir) {
        if (!hoveredStack.isEmpty() && !boundSlot.canInsert(hoveredStack)) {
            cir.setReturnValue(false);
            return false;
        }
        if (!boundStack.isEmpty() && !hovered.canInsert(boundStack)) {
            cir.setReturnValue(false);
            return false;
        }
        return true;
    }

    private boolean swapOrMoveItems(MinecraftClient client, ScreenHandler handler, int invIndex, int boundSlotId, int index, ItemStack hoveredStack, ItemStack boundStack, CallbackInfoReturnable<Boolean> cir) {
        if (!hoveredStack.isEmpty() && !boundStack.isEmpty()) {
            client.interactionManager.clickSlot(handler.syncId, invIndex, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(handler.syncId, boundSlotId, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(handler.syncId, invIndex, 0, SlotActionType.PICKUP, client.player);
            cir.setReturnValue(false);
            return true;
        }
        if (!hoveredStack.isEmpty() && boundStack.isEmpty()) {
            client.interactionManager.clickSlot(handler.syncId, invIndex, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(handler.syncId, boundSlotId, 0, SlotActionType.PICKUP, client.player);
            cir.setReturnValue(false);
            return true;
        }
        if (hoveredStack.isEmpty() && !boundStack.isEmpty()) {
            client.interactionManager.clickSlot(handler.syncId, boundSlotId, 0, SlotActionType.PICKUP, client.player);
            client.interactionManager.clickSlot(handler.syncId, index, 0, SlotActionType.PICKUP, client.player);
            cir.setReturnValue(false);
            return true;
        }
        return false;
    }
}