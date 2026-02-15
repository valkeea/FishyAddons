package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.visual.FaColors;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Inject(
        method = "updateRenderState",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/EntityRenderState;displayName:Lnet/minecraft/text/Text;", opcode = org.objectweb.asm.Opcodes.PUTFIELD, shift = At.Shift.AFTER)
    )
    private void rewriteLabelText(net.minecraft.entity.Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (state.displayName != null && FaColors.shouldColor()) {
            state.displayName = FaColors.first(state.displayName);
        }
    }
}
