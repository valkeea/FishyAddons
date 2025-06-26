package me.valkeea.fishyaddons.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SoundUtil {
    private SoundUtil() {}
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void playDynamicSound(String soundId, float volume, float pitch) {
        if (mc.player == null) return;

        Identifier id = Identifier.tryParse(soundId);
        if (id == null) {
            System.err.println("[FishyAddons] Invalid sound ID: " + soundId);
            return;
        }

        SoundEvent sound = Registries.SOUND_EVENT.get(id);
        if (sound == null || Registries.SOUND_EVENT.getId(sound) == null) {
            System.err.println("[FishyAddons] Sound not found in registry: " + soundId);
            return;
        }

        float effectiveVolume = volume * 2.0F;
        mc.player.playSound(sound, effectiveVolume, pitch);
    }
}
