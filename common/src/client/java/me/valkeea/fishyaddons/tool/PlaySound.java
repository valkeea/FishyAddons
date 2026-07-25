package me.valkeea.fishyaddons.tool;

import me.valkeea.fishyaddons.feature.skyblock.timer.ChatTimers;
import me.valkeea.fishyaddons.impl.MutableSoundInstance;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class PlaySound {
    private PlaySound() {}
    public static final Identifier PROTECT_TRIGGER_ID = Identifier.fromNamespaceAndPath("fishyaddons", "protect_trigger");
    public static final SoundEvent PROTECT_TRIGGER_EVENT = SoundEvent.createVariableRangeEvent(PROTECT_TRIGGER_ID);
    private static Minecraft mc = Minecraft.getInstance();

    public static void protectTrigger() {
        amethyst(1.0F);
    }

    public static void playBindOrLock() {
        if (mc.player != null && Config.get(BooleanKey.SLOT_LOCK_AUDIO)) {
            amethyst(1.2F);
        }
    }
        
    public static void playUnbindOrUnlock() {
        if (mc.player != null && Config.get(BooleanKey.SLOT_LOCK_AUDIO)) {
            amethyst(0.8F);
        }
    }

    public static void beaconAlarm() {
        if (mc.player != null && ChatTimers.getInstance().isBeaconAlarmOn()) {
            master(SoundEvents.BELL_BLOCK, 1.0f, 1.0f, false);
        }
    }

    public static void rainAlarm() {
        if (mc.player != null) {
            master(SoundEvents.TRIDENT_RETURN, 2.0F, 0.8F, false);
        }
    }

    public static void hotspotAlarm() {
        if (mc.player != null) {
            master(SoundEvents.CONDUIT_DEACTIVATE, 1.5F, 1.0F, false);
        }
    }

    private static void amethyst(float pitch) {
        if (mc.player != null) {
            master(PROTECT_TRIGGER_EVENT, 1.0F, pitch, false);
        }
    }

    /**
     * Play a dynamic sound using volume clamped to master category.
     * 
     * @param soundId The sound identifier string
     * @param volume The raw volume (0.0-1.0)
     * @param pitch The pitch (0.5-2.0)
     * @param noRandom Whether to disable random pitch variation
     */
    public static void dynamic(String soundId, float volume, float pitch, boolean noRandom) {
        if (mc.player != null) {
            mc.getSoundManager().stop();
            
            var id = Identifier.tryParse(soundId);
            if (id == null) {
                return;
            }

            var sound = BuiltInRegistries.SOUND_EVENT.getValue(id);
            if (sound == null || BuiltInRegistries.SOUND_EVENT.getKey(sound) == null) {
                return;
            }

            master(sound, volume, pitch, noRandom);
        }
    }

    /** Play a sound using volume clamped to master category. */
    private static void master(SoundEvent soundEvent, float volume, float pitch, boolean noRandom) {
        if (mc.player != null) {

            var soundInstance = MutableSoundInstance.master(soundEvent, pitch, volume, noRandom);
            mc.getSoundManager().play(soundInstance);
        }
    }

    /**
     * Play a dynamic sound that bypasses volume settings.
     * 
     * @param soundId The sound identifier string
     * @param volume The raw volume (0.0-1.0)
     * @param pitch The pitch (0.5-2.0)
     * @param noRandom Whether to disable random pitch variation
     */
    public static void dynamicBypass(String soundId, float volume, float pitch, boolean noRandom) {
        if (mc.player != null) {
            var id = Identifier.tryParse(soundId);
            if (id == null) {
                return;
            }

            var sound = BuiltInRegistries.SOUND_EVENT.getValue(id);
            if (sound == null || BuiltInRegistries.SOUND_EVENT.getKey(sound) == null) {
                return;
            }

            bypass(sound, volume, pitch, noRandom);
        }
    }

    /**
     * Play a sound that bypasses Minecraft's volume settings (master/category volume).
     * The sound will play at the specified volume regardless of user's volume settings.
     * 
     * @param soundEvent The sound event to play
     * @param volume The raw volume (0.0-1.0)
     * @param pitch The pitch (0.5-2.0)
     * @param noRandom Whether to disable random pitch variation
     */
    public static void bypass(SoundEvent soundEvent, float volume, float pitch, boolean noRandom) {
        if (mc.player != null) {
            var soundInstance = new MutableSoundInstance.Builder(soundEvent)
                .volume(volume)
                .pitch(pitch)
                .noRandom(noRandom)
                .bypassVolumeSettings(true)
                .build();
            
            mc.getSoundManager().play(soundInstance);
        }
    }    

    public static void repeating(SoundEvent soundEvent, float volume, float pitch, int repeatDelay) {
        if (mc.player != null) {

            var soundInstance = new MutableSoundInstance.Builder(soundEvent)
                .volume(volume)
                .pitch(pitch)
                .repeatable(true)
                .repeatDelay(repeatDelay)
                .build();
            mc.getSoundManager().play(soundInstance);
        }
    }    
}
