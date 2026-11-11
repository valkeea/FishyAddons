package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.Window;

import me.valkeea.fishyaddons.feature.qol.input.ContextBoundary;

@Mixin(Window.class)
public class MixinWindowFocus {
    @Inject(method = "onFocus", at = @At("RETURN"))
    private void onWindowFocus(boolean focused, CallbackInfo ci) {
        ContextBoundary.onWindowFocusChanged(focused);
    }
}
