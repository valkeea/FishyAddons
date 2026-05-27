package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import me.valkeea.fishyaddons.feature.skyblock.FishingHotspot;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

@Mixin(EntityRenderer.class)
public class MixinArmorStandEntityRenderer {

    @Inject(
        method = "submitNameDisplay",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cancelHspt(
        EntityRenderState state,
        PoseStack matrices,
        SubmitNodeCollector nodes,
        CameraRenderState cameraRenderState,
        CallbackInfo ci
    ) {
        if (!(state instanceof ArmorStandRenderState asrs)) return;
        if (asrs.nameTag == null) return;

        String labelText = asrs.nameTag.getString();
        if (FishingHotspot.shouldHide(labelText)) {
            ci.cancel();
        }
    }
}
