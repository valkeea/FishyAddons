package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.KeyInputEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboard {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
        method = "keyPress",
        at = @At("HEAD"),
        cancellable = true
    )
    private void faKeyPress(long handle, int action, KeyEvent input, CallbackInfo ci) {
        if (handle != minecraft.getWindow().handle()) return;
        var observed = new KeyInputEvent(minecraft, McApi.screen(), input, action);
        FaEvents.KEYBOARD_OBSERVE.broadcast(observed);
        
        var cancellable = new KeyInputEvent(minecraft, McApi.screen(), input, action);
        FaEvents.KEYBOARD_INPUT.fire(cancellable);

        if (cancellable.isConsumed()) ci.cancel();
    }
}
