package me.wait.fishymixin.mixin;

import me.wait.fishymixin.accessor.ParticleColorAccessor;
import net.minecraft.client.particle.EntityFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mixin(EntityFX.class)
public abstract class MixinEntityFX implements ParticleColorAccessor {
    @Shadow protected float field_70552_h;
    @Shadow protected float field_70553_i;
    @Shadow protected float field_70551_j;

    @Override
    public void setParticleColor(float[] rgb) {
        this.field_70552_h = rgb[0];
        this.field_70553_i = rgb[1];
        this.field_70551_j = rgb[2];
    }
}
