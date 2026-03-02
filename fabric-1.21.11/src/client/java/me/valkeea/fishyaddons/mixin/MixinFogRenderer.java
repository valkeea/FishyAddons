package me.valkeea.fishyaddons.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.visual.RenderTweaks;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.FogModifier;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;

@Mixin(FogRenderer.class)
public abstract class MixinFogRenderer {
 
    @Redirect(
        method = {"getFogColor", "applyFog"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;getCameraSubmersionType(Lnet/minecraft/client/render/Camera;)Lnet/minecraft/block/enums/CameraSubmersionType;")
    )
    private CameraSubmersionType redirectGetCameraSubmersionType(FogRenderer instance, Camera camera) {
        var originalType = this.getCameraSubmersionTypeOriginal(camera);
        var tint = RenderTweaks.shouldRemoveLavaFog(camera);
        
        if (tint != 0 && tint != 1 && originalType.equals(CameraSubmersionType.LAVA)) {
            return CameraSubmersionType.WATER;
        }
        
        return originalType;
    }

    @Redirect(
        method = "getFogColor",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogModifier;getFogColor(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/client/render/Camera;IF)I")
    )
    private int redirectFogModifierColor(FogModifier modifier, ClientWorld world, Camera camera, int viewDistance, float skyDarkness) {

        int originalColor = modifier.getFogColor(world, camera, viewDistance, skyDarkness);
        var tint = RenderTweaks.shouldRemoveLavaFog(camera);

        if (tint != 0 && tint != 1 && modifier.shouldApply(CameraSubmersionType.WATER, camera.getFocusedEntity())) {
            return tint;
        }
        
        return originalColor;
    }

    @Redirect(
        method = "applyFog",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogModifier;applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V")
    )
    private void redirectFogModifierCall(FogModifier modifier, FogData data, Camera camera, ClientWorld world, float viewDistance, RenderTickCounter tickCounter) {
        
        modifier.applyStartEndModifier(data, camera, world, viewDistance, tickCounter);
        
        var entity = camera.getFocusedEntity();
        if (RenderTweaks.shouldRemoveWaterFog(camera) && modifier.shouldApply(CameraSubmersionType.WATER, entity)) {
            data.environmentalStart = viewDistance * 2.0f;
        }
        
        var tint = RenderTweaks.shouldRemoveLavaFog(camera);
        if (tint == 1 && modifier.shouldApply(CameraSubmersionType.LAVA, entity)) {
            data.environmentalStart = viewDistance * 2.0f;
        }
        
        else if (tint != 0 && modifier.shouldApply(CameraSubmersionType.WATER, entity)) {
            data.environmentalStart = 6.0f;
            data.environmentalEnd = data.environmentalEnd * 1.5f;
        }
    }

    @Inject(
        method = "applyFog",
        at = @At("RETURN"),
        cancellable = true
    )
	public void onApplyFogReturn(
        Camera camera,
        int viewDistance,
        RenderTickCounter renderTickCounter,
        float f,
        ClientWorld clientWorld,
        CallbackInfoReturnable<Vector4f> cir
    ) {
        if (RenderTweaks.shouldRemoveWaterFog(camera)) {
            cir.setReturnValue(new Vector4f(0.8f, 0.9f, 1.0f, 0.1f));
        }        

        if (RenderTweaks.shouldRemoveLavaFog(camera) != 0) {
            cir.setReturnValue(new Vector4f(0.1f, 0.0f, 0.0f, 0.1f));
        }
    }

    private CameraSubmersionType getCameraSubmersionTypeOriginal(Camera camera) {
        var cameraSubmersionType = camera.getSubmersionType();
        return cameraSubmersionType == CameraSubmersionType.NONE ? CameraSubmersionType.ATMOSPHERIC : cameraSubmersionType;
    }
}
