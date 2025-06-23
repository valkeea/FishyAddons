package me.valkeea.fishyaddons.mixin;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.handler.SkyblockCleaner;


@Mixin(SoundManager.class)
public class MixinSoundManager {

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
    private void onPlay(SoundInstance sound, CallbackInfo ci) {
        Identifier soundId = sound.getId();
        if (SkyblockCleaner.shouldCleanHype(soundId) || SkyblockCleaner.shouldMutePhantom(soundId)) {
            ci.cancel();
        }
    }
}