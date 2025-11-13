package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.item.safeguard.GuiHandler;
import me.valkeea.fishyaddons.feature.item.safeguard.SlotHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenKey {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen == null) return;

        HandledScreen<?> gui = (HandledScreen<?>) (Object) this;
        Slot hoveredSlot = ((HandledScreenAccessor) gui).getFocusedSlot();

        if (hoveredSlot == null || !hoveredSlot.hasStack()) return;

        boolean isThrowKey = mc.options.dropKey.matchesKey(keyCode, scanCode);
        int slotId = hoveredSlot.id;
        int invIndex = SlotHandler.remap(gui, slotId);

        if (invIndex < 8 || invIndex >= 44) return;

        if (isThrowKey && (SlotHandler.isSlotLocked(invIndex) || SlotHandler.isSlotBound(invIndex))) {
            cir.setReturnValue(true);
            return;
        }

        var stack = hoveredSlot.getStack();
        if (isThrowKey && me.valkeea.fishyaddons.feature.item.safeguard.ItemHandler.isProtected(stack)) {
            GuiHandler.triggerProtection();
            cir.setReturnValue(true);
        }
    }
}
