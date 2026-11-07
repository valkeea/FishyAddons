package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.skyblock.EntityHighlight;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;

@Mixin(DebugRenderer.class)
public class MixinDebugRenderer {

    @Inject(method = "emitGizmos", at = @At("TAIL"))
    private void faEmitGizmos(Frustum frustum, double camX, double camY, double camZ, float partialTicks, CallbackInfo ci) {
        EntityHighlight.emit(frustum, partialTicks);
    }
}
