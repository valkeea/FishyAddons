package me.valkeea.fishyaddons.feature.skyblock;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.impl.MutableSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Replaces Hypixel's fishing catch sound.
 * Validation is done by recording the pitch of incoming sound packets and bobber render state.
 */
public class CatchAlert {
    private CatchAlert() {}

    private static final String DEF = "block.note_block.pling";
    private static final float PITCH_REF = 1.0f;    
    private static final int DROP_WINDOW = 15;
    private static final int RENDER_TIMEOUT = 5;
    private static final int INACTIVE_LIMIT = RENDER_TIMEOUT + 1;

    private static int catchCd = 0;
    private static int sinceRender = INACTIVE_LIMIT;
    private static float lastPitch = -1.0f;
    private static float volume = 0.0f;
    private static boolean noRandom = false;
    private static boolean trueVol = false;
    private static String id = DEF;

    public static void init() {
        refresh();
    }    

    public static void tick() {
        if (isEnabled()) {
            if (catchCd > 0) catchCd--;
            if (sinceRender < INACTIVE_LIMIT) sinceRender++;
        }
    }

    /**
     * Called every frame when player's fishing line is being rendered.
     */
    public static void onFishingLineRendered() {
        sinceRender = 0;
    }

    private static boolean isEnabled() {
        return volume > 0.0f;
    }

    /**
     * Record pitch from incoming sound packet.
     */
    public static void recordPitch(String soundId, float pitch) {
        if (soundId.contains(DEF)) lastPitch = pitch;
    }

    /**
     * Return a replacement sound instance if this is a catch alert, null otherwise.
     */
    @Nullable
    public static SoundInstance getReplacementSound(Identifier soundId) {
        if (!isFishing() || !isFishingPling(soundId.getPath())) return null;

        catchCd = DROP_WINDOW;

        var soundIdentifier = Identifier.tryParse(id);
        if (soundIdentifier == null) return null;

        var sound = Registries.SOUND_EVENT.get(soundIdentifier);
        if (sound == null || Registries.SOUND_EVENT.getId(sound) == null) return null;

        return trueVol 
            ? MutableSoundInstance.masterBypass(sound, PITCH_REF, volume, noRandom)
            : MutableSoundInstance.master(sound, PITCH_REF, volume, noRandom);
    }

    private static boolean isFishing() {
        return isEnabled() && sinceRender < RENDER_TIMEOUT;
    }

    private static boolean isFishingPling(String path) {
        if (catchCd > 0 || lastPitch < 0.0f || !path.equals(DEF)) return false;

        boolean matches = Math.abs(lastPitch - PITCH_REF) < 0.001f;
        if (matches) lastPitch = -1.0f;

        return matches;
    }

    public static void refresh() {
        volume = FishyConfig.getFloat(Key.CUSTOM_REEL, PITCH_REF);
        id = FishyConfig.getString(Key.REEL_ALERT, DEF);
        noRandom = FishyConfig.getState(Key.REEL_NORANDOM, false);
        trueVol = FishyConfig.getState(Key.REEL_TRUE_VOLUME, false);
    }
}
