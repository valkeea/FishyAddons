package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
        var tint = RenderTweaks.shouldRemoveLavaFog(cam);
        
        if (tint != 0 && tint != 1 && originalType.equals(FogType.LAVA)) {
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

        var tint = RenderTweaks.shouldRemoveLavaFog(cam);
        return tint != 0 && tint != 1 && env.isApplicable(FogType.WATER, entity) ? tint : originalColor;
    }

    @Redirect(
        method = "setupFog",
        at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/fog/environment/FogEnvironment;setupFog(Lnet/minecraft/client/renderer/fog/FogData;Lnet/minecraft/client/Camera;Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/DeltaTracker;)V")
    )
    private void redirectFogModifierCall(FogEnvironment env, FogData data, Camera cam, ClientLevel level, float viewDistance, DeltaTracker dt) {
        env.setupFog(data, cam, level, viewDistance, dt);
        
        var entity = cam.entity();
        if (entity == null) return;

        var tint = RenderTweaks.shouldRemoveLavaFog(cam);

        // 1 = remove
        if (tint == 1 && env.isApplicable(FogType.LAVA, entity)) {
            data.environmentalStart = viewDistance * 2.0f;

        } else if (env.isApplicable(FogType.WATER, entity)) {
            
            if (RenderTweaks.shouldRemoveWaterFog(cam)) {
                data.environmentalStart = viewDistance * 2.0f;
            } else if (tint != 0) { // 0 = nothing, otherwise tinted fog
                data.environmentalStart = 6.0f;
                data.environmentalEnd = data.environmentalEnd * 1.5f;
            }
        }
    }

    private FogType getCameraSubmersionTypeOriginal(Camera cam) {
        var cameraSubmersionType = cam.getFluidInCamera();
        return cameraSubmersionType == FogType.NONE ? FogType.ATMOSPHERIC : cameraSubmersionType;
    }
}
