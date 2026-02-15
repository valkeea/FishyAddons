package me.valkeea.fishyaddons.mixin;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import me.valkeea.fishyaddons.feature.visual.MobAnimations;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;

@Mixin(EntityRenderManager.class)
public class MixinEntityRenderManager {

    @Redirect(
        method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/state/CameraRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/EntityRenderState;onFire:Z", opcode = Opcodes.GETFIELD)
    )
    private boolean disableFire(EntityRenderState renderState) {
        if (MobAnimations.isFireAni()) {
            return false;
        }
        return renderState.onFire;
    }
}
