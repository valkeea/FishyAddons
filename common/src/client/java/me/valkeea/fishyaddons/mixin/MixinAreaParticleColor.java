package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.feature.visual.ato.AreaTextureOverrides;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;

@Mixin(ParticleEngine.class)
public class MixinAreaParticleColor {

    /**
     * Applies a per-area color tint (see {@link AreaTextureOverrides#remapColor}) to any
     * {@link SingleQuadParticle} on spawn, keyed by the particle's registry type id.
     */
    @SuppressWarnings("squid:S107")
    @ModifyReturnValue(
        method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
        at = @At("RETURN")
    )
    private Particle applyAreaColor(Particle original, ParticleOptions parameters, double x, double y, double z,
                                     double xa, double ya, double za) {
        if (!(original instanceof SingleQuadParticle quad)) return original;

        var island = SkyblockAreas.getIsland();
        if (!AreaTextureOverrides.hasParticleColors(island)) return original;

        var type = BuiltInRegistries.PARTICLE_TYPE.getKey(parameters.getType());
        var color = type == null ? null : AreaTextureOverrides.remapColor(island, type);

        if (color != null) {
            quad.setColor(color[0], color[1], color[2]);
        }

        return original;
    }
}
