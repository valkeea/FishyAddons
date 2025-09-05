package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@Mixin(HandledScreen.class)
public class MixinHandledScreenShift {

    @Inject(
        method = "mouseClicked",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V"
        ),
        cancellable = true
    )
    private void redirectBlockedShiftClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        HandledScreen<?> screen = (HandledScreen<?>)(Object)this;
        Slot hovered = ((HandledScreenAccessor)screen).getFocusedSlot();
        if (hovered == null) return;

        if (me.valkeea.fishyaddons.handler.GuiIcons.handleShift(hovered.id)) {
            int keyCode = 340;
            MinecraftClient cl = MinecraftClient.getInstance();
            if (cl.options != null) {
                keyCode = cl.options.sneakKey.getDefaultKey().getCode();
            }
            long handle = cl.getWindow().getHandle();
            boolean shiftDown = InputUtil.isKeyPressed(handle, keyCode);

            if (shiftDown) {
                ((HandledScreenAccessor) this).callOnMouseClick(hovered, hovered.id, button, SlotActionType.PICKUP);
                cir.setReturnValue(true);
            }
        }
    }
}