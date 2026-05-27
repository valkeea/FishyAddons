package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import me.valkeea.fishyaddons.bridge.ParticleScaleAccessor;
import net.minecraft.client.particle.SingleQuadParticle;

@Mixin(SingleQuadParticle.class)
public abstract class MixinBillboardParticle implements ParticleScaleAccessor {
    @Shadow protected float quadSize;

    @Override
    public void setParticleScale(float scale) {
        this.quadSize = scale;
    }
}
