package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.bridge.ParticleColorAccessor;
import me.valkeea.fishyaddons.bridge.ParticleScaleAccessor;
import me.valkeea.fishyaddons.feature.visual.ParticleVisuals;
import net.minecraft.client.particle.DamageParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;

@Mixin(DamageParticle.Factory.class)
public class MixinDamageParticleFactory {

    @SuppressWarnings("squid:S107")
    @Inject(
        method = "createParticle",
        at = @At("TAIL")
    )
    private void onCreateParticle(SimpleParticleType parameters, ClientWorld world,
                                  double x, double y, double z,
                                  double velocityX, double velocityY, double velocityZ,
                                  CallbackInfoReturnable<Particle> cir) {

        if (world == null || parameters == null || !ParticleVisuals.getDmg()) return;

        if (parameters.getType() == ParticleTypes.CRIT) {

            Particle particle = cir.getReturnValue();
            if (particle instanceof ParticleScaleAccessor accessor) {
                accessor.setParticleScale(ParticleVisuals.cachedScale());
            }

            if (particle instanceof ParticleColorAccessor color) {
                float [] custom = ParticleVisuals.getCustomColor();
                color.setColor(custom[0], custom[1], custom[2]);
            }
        }
    }
}
