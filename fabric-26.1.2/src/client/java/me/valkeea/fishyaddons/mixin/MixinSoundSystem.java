package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.skyblock.SkyblockCleaner;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;

@Mixin(SoundEngine.class)
public class MixinSoundSystem {

    @ModifyVariable(
        method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
        at = @At("HEAD"),
        argsOnly = true
    )
    private SoundInstance modifySound(SoundInstance sound) {
        var replacement = SkyblockCleaner.getReplacementSound(sound);
        return replacement != null ? replacement : sound;
    }

    @Inject(
        method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cancelSound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (SkyblockCleaner.shouldClean(sound)) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
