package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.bridge.ParticleColorAccessor;
import me.valkeea.fishyaddons.handler.RedstoneColor;
import net.minecraft.client.particle.RedDustParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;

@Mixin(RedDustParticle.class)
public abstract class MixinRedDustParticle {

    @Inject(
        method = "<init>(Lnet/minecraft/client/world/ClientWorld;DDD" +
                 "DDD" +
                 "Lnet/minecraft/particle/DustParticleEffect;" +
                 "Lnet/minecraft/client/particle/SpriteProvider;)V",
        at = @At("TAIL")
    )
    private void onConstruct(ClientWorld world, double x, double y, double z,
                             double velocityX, double velocityY, double velocityZ,
                             DustParticleEffect parameters, SpriteProvider spriteProvider,
                             CallbackInfo ci) {
        if (RedstoneColor.cachedIndex() == 0) return;

        float[] custom = RedstoneColor.getCustomColor();
        if (custom != null) {
            ((ParticleColorAccessor) this).setColor(custom[0], custom[1], custom[2]);
        }
    }
}
