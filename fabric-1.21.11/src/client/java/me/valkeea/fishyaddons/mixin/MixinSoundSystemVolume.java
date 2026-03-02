package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import me.valkeea.fishyaddons.impl.BypassVolumeSound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.util.math.MathHelper;
    
@Mixin(value = SoundSystem.class, priority = 1100)
public class MixinSoundSystemVolume {

    /**
     * If this is a BypassVolumeSound, modify the claculated volume (h) after getAdjustedVolume is called.
     */
    @ModifyVariable(
        method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;",
        at = @At(value = "STORE"),
        ordinal = 2
    )
    private float modifyCalculatedVolume(float h, SoundInstance sound) {
        if (sound instanceof BypassVolumeSound bvs && bvs.shouldBypassVolumeSettings()) {
            return MathHelper.clamp(sound.getVolume(), 0.0F, 2.0F);
        }
        return h;
    }
}
