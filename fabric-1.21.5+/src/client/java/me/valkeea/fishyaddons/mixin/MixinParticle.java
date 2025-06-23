package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import me.valkeea.fishyaddons.bridge.ParticleColorAccessor;
import net.minecraft.client.particle.Particle;

@Mixin(Particle.class)
public abstract class MixinParticle implements ParticleColorAccessor {
    @Shadow protected float red;
    @Shadow protected float green;
    @Shadow protected float blue;

    @Override
    public void setColor(float r, float g, float b) {
        this.red = r;
        this.green = g;
        this.blue = b;
    }
}