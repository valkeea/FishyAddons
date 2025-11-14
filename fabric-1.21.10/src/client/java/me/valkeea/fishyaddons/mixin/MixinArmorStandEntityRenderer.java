package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.skyblock.FishingHotspot;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;

@Mixin(EntityRenderer.class)
public class MixinArmorStandEntityRenderer {

    @Inject(
        method = "renderLabelIfPresent",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cancelHspt(
        EntityRenderState state,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        CameraRenderState cameraRenderState,
        CallbackInfo ci
    ) {
        if (!(state instanceof ArmorStandEntityRenderState armorStandState)) return;
        if (armorStandState.displayName == null) return;

        String labelText = armorStandState.displayName.getString();
        if (FishingHotspot.shouldHide(labelText)) {
            ci.cancel();
        }
    }
}
