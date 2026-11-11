package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.Window;

import me.valkeea.fishyaddons.feature.qol.input.ContextBoundary;

@Mixin(Window.class)
public class MixinWindowFocus {
    @Shadow
    @Final
    private long handle;

    @Inject(method = "onFocus", at = @At("RETURN"))
    private void onWindowFocus(long window, boolean focused, CallbackInfo ci) {
        if (window == handle) ContextBoundary.onWindowFocusChanged(focused);
    }
}
