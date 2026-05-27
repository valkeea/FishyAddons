package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.visual.ParticleVisuals;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DustParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.DustParticleOptions;

@Mixin(DustParticle.class)
public abstract class MixinRedDustParticle extends SingleQuadParticle {
    protected MixinRedDustParticle(ClientLevel level, double x, double y, double z,
                                   TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
    }

    @SuppressWarnings("squid:S107")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(ClientLevel level, double x, double y, double z,
                        double vx, double vy, double vz,
                        DustParticleOptions parameters, SpriteSet spriteProvider,
                        CallbackInfo ci) {
               
        float[] color = ParticleVisuals.getCustomColor(this.rCol, this.gCol, this.bCol);

        if (color.length == 3) {
            this.rCol = color[0];
            this.gCol = color[1];
            this.bCol = color[2];
        }
    }
}
