package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.feature.visual.MobAnimations;
import me.valkeea.fishyaddons.feature.visual.ato.AreaTextureOverrides;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<S extends LivingEntityRenderState> {

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void skipDeadEntityRendering(S state, PoseStack matrixStack, SubmitNodeCollector nodes, CameraRenderState cam, CallbackInfo ci) {
        if (state.deathTime > 0.0F && MobAnimations.isDeathAni()) {
            ci.cancel();
        }
    }

    @Redirect(
        method = "getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getTextureLocation(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Lnet/minecraft/resources/Identifier;"
        )
    )
    private Identifier remapAreaEntityTexture(LivingEntityRenderer<?, S, ?> renderer, S state) {
        var original = renderer.getTextureLocation(state);
        var remapped = AreaTextureOverrides.remap(SkyblockAreas.getIsland(), original);
        return remapped == null ? original : remapped;
    }
}
