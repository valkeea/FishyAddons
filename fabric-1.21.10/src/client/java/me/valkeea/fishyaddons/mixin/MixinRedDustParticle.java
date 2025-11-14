package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.visual.ParticleVisuals;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.RedDustParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;

@Mixin(RedDustParticle.class)
public abstract class MixinRedDustParticle extends BillboardParticle {
    protected MixinRedDustParticle(ClientWorld world, double x, double y, double z,
                                   Sprite sprite) {
        super(world, x, y, z, sprite);
    }

    @SuppressWarnings("squid:S107")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(ClientWorld world, double x, double y, double z,
                        double vx, double vy, double vz,
                        DustParticleEffect parameters, SpriteProvider spriteProvider,
                        CallbackInfo ci) {

        var color = ParticleVisuals.getCustomColor();
        if (color != null) {
            this.red = color[0];
            this.green = color[1];
            this.blue = color[2];
        }
    }
}
