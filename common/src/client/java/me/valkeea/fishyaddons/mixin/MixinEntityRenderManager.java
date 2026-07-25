package me.valkeea.fishyaddons.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import me.valkeea.fishyaddons.feature.visual.MobAnimations;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderManager {

    @Redirect(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/level/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;displayFireAnimation:Z",
        opcode = Opcodes.GETFIELD
    ))
    private boolean disableFire(EntityRenderState renderState) {
        if (MobAnimations.isFireAni()) {
            return false;
        }
        return renderState.displayFireAnimation;
    }
}
