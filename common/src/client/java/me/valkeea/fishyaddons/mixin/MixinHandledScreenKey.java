package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.feature.item.safeguard.FGUtil;
import me.valkeea.fishyaddons.feature.item.safeguard.SlotHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreenKey {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !McApi.screenIsActive()) return;

        var gui = (AbstractContainerScreen<?>) (Object) this;
        var hoveredSlot = ((HandledScreenAccessor) gui).getHoveredSlot();

        if (hoveredSlot == null || !hoveredSlot.hasItem()) return;

        boolean isThrowKey = mc.options.keyDrop.matches(input);
        int slotId = hoveredSlot.index;
        int invIdx = SlotHandler.remap(gui, slotId);
        if (invIdx == -1) return;

        if (isThrowKey && FGUtil.preventSlotClick(invIdx)) {
            cir.setReturnValue(true);
            return;
        }

        var stack = hoveredSlot.getItem();
        if (isThrowKey && FGUtil.isProtected(stack)) {
            FGUtil.triggerProtection();
            cir.setReturnValue(true);
        }
    }
}
