package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.bridge.ParticleScaleAccessor;
import me.valkeea.fishyaddons.feature.visual.ParticleVisuals;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CritParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

@Mixin(CritParticle.Provider.class)
public class MixinDamageParticleFactory {

    @SuppressWarnings("squid:S107")
    @Inject(method = "createParticle", at = @At("TAIL"))
    private void onCreateParticle(SimpleParticleType parameters, ClientLevel level,
                                    double d, double e, double f, double g, double h, double i,
                                    RandomSource random, CallbackInfoReturnable<Particle> cir) {

        if (level == null || parameters == null || !ParticleVisuals.getDmg()) return;
                                  
        if (parameters.getType() == ParticleTypes.CRIT) {
            Particle p = cir.getReturnValue();

            if (p instanceof ParticleScaleAccessor accessor) {
                accessor.setParticleScale(ParticleVisuals.cachedScale());
            }
        }
    }
}
