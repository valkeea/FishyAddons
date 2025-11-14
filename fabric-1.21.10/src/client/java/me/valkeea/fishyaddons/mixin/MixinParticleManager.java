package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.skyblock.SkyblockCleaner;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

@Mixin(ParticleManager.class)
public class MixinParticleManager {

    @SuppressWarnings("squid:S107")
    @Inject(
        method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onAddParticle(ParticleEffect parameters, double x, double y, double z, double velocityX,
                                double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        try {
            if (SkyblockCleaner.shouldCleanHype() && 
                (parameters.getType() == ParticleTypes.EXPLOSION
                || parameters.getType() == ParticleTypes.EXPLOSION_EMITTER)) {
                cir.cancel();
            }
        } catch (Exception e) {
            // Silent exit
        }
    }
}
