package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.bridge.ParticleScaleAccessor;
import me.valkeea.fishyaddons.feature.visual.ParticleVisuals;
import net.minecraft.client.particle.DamageParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

@Mixin(DamageParticle.Factory.class)
public class MixinDamageParticleFactory {

    @SuppressWarnings("squid:S107")
    @Inject(method = "createParticle", at = @At("TAIL"))
    private void onCreateParticle(SimpleParticleType parameters, ClientWorld world,
                                    double d, double e, double f, double g, double h, double i,
                                    Random random, CallbackInfoReturnable<Particle> cir) {

        if (world == null || parameters == null || !ParticleVisuals.getDmg()) return;
                                  
        if (parameters.getType() == ParticleTypes.CRIT) {
            Particle particle = cir.getReturnValue();

            if (particle instanceof ParticleScaleAccessor accessor) {
                accessor.setParticleScale(ParticleVisuals.cachedScale());
            }
        }
    }
}
