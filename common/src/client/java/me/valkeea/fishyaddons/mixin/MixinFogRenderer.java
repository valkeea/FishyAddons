package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.visual.RenderTweaks;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.world.level.material.FogType;

@Mixin(FogRenderer.class)
public abstract class MixinFogRenderer {
 
    @Redirect(
        method = {"computeFogColor", "setupFog"},
        at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/fog/FogRenderer;getFogType(Lnet/minecraft/client/Camera;)Lnet/minecraft/world/level/material/FogType;")
    )
    private FogType redirectGetCameraSubmersionType(FogRenderer instance, Camera cam) {
        var originalType = this.getCameraSubmersionTypeOriginal(cam);
        var modifier = RenderTweaks.getFogModifier(cam);
        
        if (modifier.isTint() && originalType.equals(FogType.LAVA)) {
            return FogType.WATER;
        }
        
        return originalType;
    }

    @Redirect(
        method = "computeFogColor",
        at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/fog/environment/FogEnvironment;getBaseColor(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/Camera;IF)I")
    )
    private int redirectFogModifierColor(FogEnvironment env, ClientLevel level, Camera cam, int viewDistance, float skyDarkness) {

        var entity = cam.entity();
        int originalColor = env.getBaseColor(level, cam, viewDistance, skyDarkness); 
        if (entity == null) return originalColor;

        var modifier = RenderTweaks.getFogModifier(cam);
        boolean applicable = env.isApplicable(FogType.ATMOSPHERIC, entity) && modifier.isSky()
            || env.isApplicable(FogType.WATER, entity) && modifier.isTint();

        return applicable ? modifier.color() : originalColor;
    }

    @Inject(
        method = "setupFog",
        at = @At("RETURN")
    )
    private void adjustFogData(
        Camera cam, int renderDistanceInChunks, DeltaTracker dt, float darkenWorldAmount,
        ClientLevel level, CallbackInfoReturnable<FogData> cir
    ) {

        var entity = cam.entity();
        if (entity == null) return;

        var data = cir.getReturnValue();
        var viewDistance = renderDistanceInChunks * 16.0f;
        var modifier = RenderTweaks.getFogModifier(cam);
        var fluid = cam.getFluidInCamera();

        if (modifier.isRemove() && fluid == FogType.LAVA) {
            data.environmentalStart = viewDistance;
            data.environmentalEnd = viewDistance;

        } else if (fluid == FogType.WATER) {
            if (RenderTweaks.shouldRemoveWaterFog(cam)) {
                data.environmentalStart = viewDistance;
                data.environmentalEnd = viewDistance;
            }

        } else if (modifier.isTint() && fluid == FogType.LAVA) {
            data.environmentalStart = 6.0f;
            data.environmentalEnd = data.environmentalEnd * 1.5f;
        }
    }

    private FogType getCameraSubmersionTypeOriginal(Camera cam) {
        var cameraSubmersionType = cam.getFluidInCamera();
        return cameraSubmersionType == FogType.NONE ? FogType.ATMOSPHERIC : cameraSubmersionType;
    }
}
