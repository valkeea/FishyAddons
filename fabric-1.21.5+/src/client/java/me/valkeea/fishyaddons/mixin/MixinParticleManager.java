package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.handler.SkyblockCleaner;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

@Mixin(ParticleManager.class)
public class MixinParticleManager {

    @Inject(method =
    "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
    at = @At("HEAD"), cancellable = true)
    private void onAddParticle(
        ParticleEffect parameters, double x, double y, double z, double velocityX,
        double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        try {
            if (SkyblockCleaner.shouldCleanHype() && 
                (parameters.getType() == ParticleTypes.EXPLOSION
                || parameters.getType() == ParticleTypes.EXPLOSION_EMITTER)) {
                // Instead of returning null, cancel the method to prevent particle creation
                // This is safer than returning null which can cause issues downstream
                cir.cancel();
            }
        } catch (Exception e) {
            // Silently handle any errors to prevent particle system crashes
        }
    }
}