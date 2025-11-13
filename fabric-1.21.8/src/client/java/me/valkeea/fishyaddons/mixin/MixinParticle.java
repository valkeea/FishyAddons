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
        // Validate input values to prevent invalid particle states
        if (Float.isNaN(r) || Float.isNaN(g) || Float.isNaN(b) ||
            Float.isInfinite(r) || Float.isInfinite(g) || Float.isInfinite(b)) {
            return; // Skip setting invalid colors
        }
        
        this.red = Math.clamp(r, 0.0f, 1.0f);
        this.green = Math.clamp(g, 0.0f, 1.0f);
        this.blue = Math.clamp(b, 0.0f, 1.0f);
    }
}