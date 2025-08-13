package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.safeguard.SellProtectionHandler;
import me.valkeea.fishyaddons.safeguard.SlotProtectionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.slot.Slot;


@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenKey {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen == null) return;

        HandledScreen<?> gui = (HandledScreen<?>) (Object) this;
        Slot hoveredSlot = ((HandledScreenAccessor) gui).getFocusedSlot();

        if (hoveredSlot == null || !hoveredSlot.hasStack()) return;

        boolean isThrowKey = mc.options.dropKey.matchesKey(keyCode, scanCode);
        int slotId = hoveredSlot.id;
        int invIndex = SlotProtectionManager.remap(gui, slotId);

        // Only apply to player inventory/armor slots
        if (invIndex < 8 || invIndex >= 44) {
            return;
        }
        if (isThrowKey && (SlotProtectionManager.isSlotLocked(invIndex)
                || SlotProtectionManager.isSlotBound(invIndex))) {
            cir.setReturnValue(true);
            return;
        }

        // UUID-based protection
        ItemStack stack = hoveredSlot.getStack();
        RegistryWrapper.WrapperLookup registries = mc.world != null ? mc.world.getRegistryManager() : null;
        if (isThrowKey && me.valkeea.fishyaddons.safeguard.ItemHandler.isProtected(stack, registries)) {
            SellProtectionHandler.triggerProtection();
            cir.setReturnValue(true);
        }
    }
}
