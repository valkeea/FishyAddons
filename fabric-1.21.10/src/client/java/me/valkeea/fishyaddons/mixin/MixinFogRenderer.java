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
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

@Mixin(FogRenderer.class)
public abstract class MixinFogRenderer {
 
    @Redirect(
        method = {"getFogColor", "applyFog"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;getCameraSubmersionType(Lnet/minecraft/client/render/Camera;Z)Lnet/minecraft/block/enums/CameraSubmersionType;")
    )
    private CameraSubmersionType redirectGetCameraSubmersionType(FogRenderer instance, Camera camera, boolean thick) {
        var originalType = this.getCameraSubmersionTypeOriginal(camera, thick);
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

        if (tint != 0 && tint != 1) {
            // Submersion type is already WATER
            if (modifier.shouldApply(CameraSubmersionType.WATER, camera.getFocusedEntity())) {
                return tint;
            }
        }
        
        return originalColor;
    }

    @Redirect(
        method = "applyFog",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogModifier;applyStartEndModifier(Lnet/minecraft/client/render/fog/FogData;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/world/ClientWorld;FLnet/minecraft/client/render/RenderTickCounter;)V")
    )
    private void redirectFogModifierCall(FogModifier modifier, FogData data, Entity entity, BlockPos pos, ClientWorld world, float viewDistance, RenderTickCounter tickCounter,
                                       Camera camera, int viewDistanceParam, boolean thick, RenderTickCounter tickCounterParam, float skyDarkness, ClientWorld worldParam) {
        
        // Original modifier call first to get proper initialization
        modifier.applyStartEndModifier(data, entity, pos, world, viewDistance, tickCounter);
        
        if (RenderTweaks.shouldRemoveWaterFog(camera) && modifier.shouldApply(CameraSubmersionType.WATER, entity)) {
            data.environmentalStart = viewDistance * 2.0f;
        }
        
        var tint = RenderTweaks.shouldRemoveLavaFog(camera);
        if (tint == 1 && modifier.shouldApply(CameraSubmersionType.LAVA, entity)) {
            data.environmentalStart = viewDistance * 2.0f;
        }
        
        // Adjust density for tinting
        else if (tint != 0 && modifier.shouldApply(CameraSubmersionType.WATER, entity)) {
            data.environmentalStart = 6.0f;
            data.environmentalEnd = data.environmentalEnd * 1.5f;
        }
    }

    // Influence the way sky is tinted when viewed from under water/lava 
    @Inject(
        method = "applyFog",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onApplyFogReturn(
        Camera camera,
        int viewDistance,
        boolean thick,
        RenderTickCounter tickCounter,
        float skyDarkness,
        ClientWorld world,
        CallbackInfoReturnable<Vector4f> cir
    ) {
        if (RenderTweaks.shouldRemoveWaterFog(camera)) {
            cir.setReturnValue(new Vector4f(0.8f, 0.9f, 1.0f, 0.1f));
        }        

        if (RenderTweaks.shouldRemoveLavaFog(camera) != 0) {
            cir.setReturnValue(new Vector4f(0.1f, 0.0f, 0.0f, 0.1f));
        }
    }

    private CameraSubmersionType getCameraSubmersionTypeOriginal(Camera camera, boolean thick) {
        var cameraSubmersionType = camera.getSubmersionType();
        if (cameraSubmersionType == CameraSubmersionType.NONE) {
            return thick ? CameraSubmersionType.DIMENSION_OR_BOSS : CameraSubmersionType.ATMOSPHERIC;
        } else {
            return cameraSubmersionType;
        }
    }
}
