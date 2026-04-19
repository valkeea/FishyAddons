package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.item.safeguard.FGUtil;
import me.valkeea.fishyaddons.feature.item.safeguard.SlotHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenKey {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen == null) return;

        HandledScreen<?> gui = (HandledScreen<?>) (Object) this;
        var hoveredSlot = ((HandledScreenAccessor) gui).getFocusedSlot();

        if (hoveredSlot == null || !hoveredSlot.hasStack()) return;

        boolean isThrowKey = mc.options.dropKey.matchesKey(input);
        int slotId = hoveredSlot.id;
        int invIdx = SlotHandler.remap(gui, slotId);
        if (invIdx == -1) return;

        if (isThrowKey && FGUtil.preventSlotClick(invIdx)) {
            cir.setReturnValue(true);
            return;
        }

        var stack = hoveredSlot.getStack();
        if (isThrowKey && FGUtil.isProtected(stack)) {
            FGUtil.triggerProtection();
            cir.setReturnValue(true);
        }
    }
}
