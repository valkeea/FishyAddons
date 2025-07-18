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
        if (invIndex <= 4 || invIndex >= 44) return;
        
        boolean isLocked = SlotProtectionManager.isSlotLocked(invIndex);
        boolean isBound = SlotProtectionManager.isSlotBound(invIndex);

        if (isLocked) {
            cir.setReturnValue(false);
            return;
        }

        // Only apply "mini container" logic on shift-click for bound slots
        if (isBound) {
            boolean isShiftDown = GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                               || GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

            if (!isShiftDown) {
                // Block normal clicks on bound slots
                cir.setReturnValue(false);
                return;
            }

            int boundSlotId = SlotProtectionManager.getBoundSlot(invIndex);
            ScreenHandler handler = screen.getScreenHandler();
            if (handler == null || boundSlotId < 0 || boundSlotId >= handler.slots.size()) {
                cir.setReturnValue(false);
                return;
            }
            Slot boundSlot = handler.getSlot(boundSlotId);

            ItemStack hoveredStack = hovered.getStack();
            ItemStack boundStack = boundSlot.getStack();

            // Block if item can't go in bound slot (e.g. non-armor in armor slot)
            if (!hoveredStack.isEmpty() && !boundSlot.canInsert(hoveredStack)) {
                cir.setReturnValue(false);
                return;
            }
            if (!boundStack.isEmpty() && !hovered.canInsert(boundStack)) {
                cir.setReturnValue(false);
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();

            // If both slots have items, swap them
            if (!hoveredStack.isEmpty() && !boundStack.isEmpty()) {
                client.interactionManager.clickSlot(handler.syncId, invIndex, 0, SlotActionType.PICKUP, client.player);
                client.interactionManager.clickSlot(handler.syncId, boundSlotId, 0, SlotActionType.PICKUP, client.player);
                client.interactionManager.clickSlot(handler.syncId, invIndex, 0, SlotActionType.PICKUP, client.player);
                cir.setReturnValue(false);
                return;
            }

            // If only one has an item, move it to the empty slot
            if (!hoveredStack.isEmpty() && boundStack.isEmpty()) {
                client.interactionManager.clickSlot(handler.syncId, invIndex, 0, SlotActionType.PICKUP, client.player);
                client.interactionManager.clickSlot(handler.syncId, boundSlotId, 0, SlotActionType.PICKUP, client.player);
                cir.setReturnValue(false);
                return;
            }
            if (hoveredStack.isEmpty() && !boundStack.isEmpty()) {
                client.interactionManager.clickSlot(handler.syncId, boundSlotId, 0, SlotActionType.PICKUP, client.player);
                client.interactionManager.clickSlot(handler.syncId, index, 0, SlotActionType.PICKUP, client.player);
                cir.setReturnValue(false);
                return;
            }

            // If both are empty, do nothing
            cir.setReturnValue(false);
        }
    }
}