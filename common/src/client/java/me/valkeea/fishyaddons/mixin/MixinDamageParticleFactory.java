package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.skyblock.EntityHighlight;
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
    private void faCreateParticle(SimpleParticleType params, ClientLevel level,
                                    double x, double y, double z, double xAux, double yAux, double zAux,
                                    RandomSource r, CallbackInfoReturnable<Particle> cir) {

        if (level == null || params == null) return;
                                  
        if (params.getType() == ParticleTypes.CRIT) {
            EntityHighlight.onCritParticle(level, x, y, z);
        }
    }
}
