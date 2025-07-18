package me.valkeea.fishyaddons.mixin;

import me.valkeea.fishyaddons.bridge.ParticleScaleAccessor;
import net.minecraft.client.particle.BillboardParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BillboardParticle.class)
public abstract class MixinBillboardParticle implements ParticleScaleAccessor {
    @Shadow protected float scale;

    @Override
    public void setParticleScale(float scale) {
        this.scale = scale;
    }
}