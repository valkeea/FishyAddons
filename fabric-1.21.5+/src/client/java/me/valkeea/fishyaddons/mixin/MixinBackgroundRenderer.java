package me.valkeea.fishyaddons.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.handler.RenderTweaks;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;

@Mixin(BackgroundRenderer.class)
public abstract class MixinBackgroundRenderer {

    @Redirect(
            method = "getFogColor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;getSubmersionType()Lnet/minecraft/block/enums/CameraSubmersionType;")
    )
    private static CameraSubmersionType redirectFogColorSubmersionType(Camera camera) {
        return RenderTweaks.modifyFogSubmersionType(camera.getSubmersionType());
    }

    @Redirect(
            method = "applyFog",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;getSubmersionType()Lnet/minecraft/block/enums/CameraSubmersionType;")
    )
    private static CameraSubmersionType redirectApplyFogSubmersionType(Camera camera) {
        return RenderTweaks.modifyFogSubmersionType(camera.getSubmersionType());
    }

    @Inject(
        method = "applyFog",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void applyFog(
        Camera camera,
        BackgroundRenderer.FogType fogType,
        Vector4f color,
        float viewDistance,
        boolean thickenFog,
        float tickProgress,
        CallbackInfoReturnable<Fog> cir
    ) {
        if (RenderTweaks.shouldRemoveLavaFog(camera)) {
            // Return a "no fog" Fog instance
            cir.setReturnValue(new Fog(
                0.0f,
                1000.0f,
                FogShape.SPHERE,
                1.0f,
                1.0f,
                1.0f,
                1.0f
            ));
        }

        if (RenderTweaks.shouldRemoveWaterFog(camera)) {
            // Return a "no fog" Fog instance
            cir.setReturnValue(new Fog(
                0.0f,
                1000.0f,
                FogShape.SPHERE,
                1.0f,
                1.0f,
                1.0f,
                1.0f
            ));
        }
    }
}
