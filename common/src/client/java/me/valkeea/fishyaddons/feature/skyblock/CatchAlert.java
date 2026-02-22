package me.valkeea.fishyaddons.feature.skyblock;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.impl.MutableSoundInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Tracks fishing state to enable sound replacement for incoming catches.
 * (Contextual validation)
 */
public class CatchAlert {
    private CatchAlert() {}

    private static float volume = 0.0f;
    private static boolean noRandom = false;
    private static boolean trueVol = false;
    private static String def = "block.note_block.pling";
    private static String id = def;
    private static FishingBobberEntity lastBobber = null;
    private static int catchCd = 0;
    private static long bookCd = 0;

    public static void init() {
        refresh();
    }    

    public static void tick() {
        if (!isEnabled()) {
            lastBobber = null;
            return;
        }

        if (catchCd > 0) catchCd--;

        if (bookCd > 0) {
            if (bookCd >= 5) bookCd = 0;
            else bookCd--;
        }

        lastBobber = isFishing();
    }

    /**
     * Return the player's fishing bobber if it exists and is valid, null otherwise.
     */
    public static FishingBobberEntity isFishing() {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return null;
        return mc.player.fishHook;
    }

    public static boolean isEnabled() {
        return volume > 0.0f;
    }

    /**
     * Return a replacement sound instance if this is a catch alert, null otherwise.
     */
    @Nullable
    public static SoundInstance getReplacementSound(Identifier soundId) {
        if (!isFishingPling(soundId.getPath())) return null; 
               
        catchCd = 10;
        
        var soundIdentifier = Identifier.tryParse(id);
        if (soundIdentifier == null) return null;

        var sound = Registries.SOUND_EVENT.get(soundIdentifier);
        if (sound == null || Registries.SOUND_EVENT.getId(sound) == null) return null;

        return trueVol 
            ? MutableSoundInstance.masterBypass(sound, 1.0f, volume, noRandom)
            : MutableSoundInstance.master(sound, 1.0f, volume, noRandom);
    }

    private static boolean isFishingPling(String path) {
        return isEnabled() && catchCd + bookCd <= 0 && lastBobber != null &&
        !lastBobber.isRemoved() && path.equals(def);
    }

    public static void bookDetected() {
        bookCd = 5;
    }

    public static void refresh() {
        volume = FishyConfig.getFloat(Key.CUSTOM_REEL, 1.0f);
        id = FishyConfig.getString(Key.REEL_ALERT, def);
        noRandom = FishyConfig.getState(Key.REEL_NORANDOM, false);
        trueVol = FishyConfig.getState(Key.REEL_TRUE_VOLUME, false);
    }
}
