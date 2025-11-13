package me.valkeea.fishyaddons.mixin;

import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import me.valkeea.fishyaddons.feature.visual.RenderTweaks;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;

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
        int tint = RenderTweaks.shouldRemoveLavaFog(camera);
        if (tint != 0 && tint != 1) {
            float red = ((tint >> 16) & 0xFF) / 255.0f;
            float green = ((tint >> 8) & 0xFF) / 255.0f;
            float blue = (tint & 0xFF) / 255.0f;
            cir.setReturnValue(new Vector4f(red, green, blue, 1.0f));
        }
    }

    @ModifyArgs(
        method = "applyFog",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V")
    )
    private void modifyFogBufferArgs(Args args, Camera camera, int viewDistance, boolean thick, RenderTickCounter tickCounter, float skyDarkness, ClientWorld world) {
        
        if (RenderTweaks.shouldRemoveWaterFog(camera)) {
            args.set(3, 0.0f);
            args.set(4, 1000.0f);
            args.set(5, 0.0f);
            args.set(6, 1000.0f);
            args.set(7, 1000.0f);
            args.set(8, 1000.0f);
            return;
        }
        
        int lavaModifier = RenderTweaks.shouldRemoveLavaFog(camera);
        if (lavaModifier == 1) {
            args.set(3, 0.0f);
            args.set(4, 1000.0f);
            args.set(5, 0.0f);
            args.set(6, 1000.0f);
            args.set(7, 1000.0f);
            args.set(8, 1000.0f);
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
