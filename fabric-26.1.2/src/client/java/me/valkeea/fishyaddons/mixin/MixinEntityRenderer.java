package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.visual.FaColors;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Inject(
        method = "extractRenderState",
        at = @At(value = "FIELD",
        target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;nameTag:Lnet/minecraft/network/chat/Component;",
        opcode = org.objectweb.asm.Opcodes.PUTFIELD, shift = At.Shift.AFTER
    ))
    private void rewriteLabelText(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (state.nameTag != null && FaColors.shouldColor()) {
            state.nameTag = FaColors.first(state.nameTag);
        }
    }
}
