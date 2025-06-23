package me.wait.fishymixin.mixin;

import me.wait.fishymixin.accessor.ParticleColorAccessor;
import net.minecraft.client.particle.EntityReddustFX;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import me.wait.fishyaddons.config.ParticleColorConfig;

@SideOnly(Side.CLIENT)
@Mixin(EntityReddustFX.class)
public abstract class MixinEntityReddustFX {

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDFFF)V", at = @At("RETURN"))
    private void onInit(World world, double x, double y, double z, float r, float g, float b, CallbackInfo ci) {
        if (ParticleColorConfig.cachedIndex() == 0) {
            return;
        }

        float[] newColor = ParticleColorConfig.getCustomColor();
        if (newColor != null && ParticleColorConfig.shouldReplace(r, g, b)) {
            ((ParticleColorAccessor) this).setParticleColor(newColor);
        }
    }
}
