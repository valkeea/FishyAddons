package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.skyblock.FishingHotspot;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@Mixin(EntityRenderer.class)
public class MixinArmorStandEntityRenderer {

    @Inject(
        method = "renderLabelIfPresent",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cancelHspt(EntityRenderState renderState, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (text == null) return;
        if (!(renderState instanceof ArmorStandEntityRenderState)) return;
        
        String labelText = text.getString();
        if (FishingHotspot.shouldHide(labelText)) {
            ci.cancel();
        }
    }
}
