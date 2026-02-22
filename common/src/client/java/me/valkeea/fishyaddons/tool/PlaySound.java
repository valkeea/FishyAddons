package me.valkeea.fishyaddons.tool;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.feature.skyblock.timer.ChatTimers;
import me.valkeea.fishyaddons.impl.MutableSoundInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class PlaySound {
    private PlaySound() {}
    public static final Identifier PROTECT_TRIGGER_ID = Identifier.of("fishyaddons", "protect_trigger");
    public static final SoundEvent PROTECT_TRIGGER_EVENT = SoundEvent.of(PROTECT_TRIGGER_ID);
    private static MinecraftClient mc = MinecraftClient.getInstance();

    public static void protectTrigger() {
        if (ItemConfig.isProtectNotiEnabled()) {
            amethyst(1.0F);
        }
    }

    public static void playBindOrLock() {
        if (mc.player != null && ItemConfig.isLockTriggerEnabled()) {
            amethyst(1.2F);
        }
    }
        
    public static void playUnbindOrUnlock() {
        if (mc.player != null && ItemConfig.isLockTriggerEnabled()) {
            amethyst(0.8F);
        }
    }

    public static void beaconAlarm() {
        if (mc.player != null && ChatTimers.getInstance().isBeaconAlarmOn()) {
            master(SoundEvents.BLOCK_BELL_USE, 1.0f, 1.0f, false);
        }
    }

    public static void rainAlarm() {
        if (mc.player != null) {
            master(SoundEvents.ITEM_TRIDENT_RETURN, 2.0F, 0.8F, false);
        }
    }

    public static void hotspotAlarm() {
        if (mc.player != null) {
            master(SoundEvents.BLOCK_CONDUIT_DEACTIVATE, 1.5F, 1.0F, false);
        }
    }

    public static void cocoonAlarm() {
        if (mc.player != null) {
            master(SoundEvents.ENTITY_BREEZE_CHARGE, 3.0F, 0.2F, false);
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
        if (mc.player != null && mc.getSoundManager() != null) {
            mc.getSoundManager().stopAll();
            
            var id = Identifier.tryParse(soundId);
            if (id == null) {
                return;
            }

            var sound = Registries.SOUND_EVENT.get(id);
            if (sound == null || Registries.SOUND_EVENT.getId(sound) == null) {
                return;
            }

            master(sound, volume, pitch, noRandom);
        }
    }

    /** Play a sound using volume clamped to master category. */
    private static void master(SoundEvent soundEvent, float volume, float pitch, boolean noRandom) {
        if (mc.player != null && mc.getSoundManager() != null) {

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
        if (mc.player != null && mc.getSoundManager() != null) {
            var id = Identifier.tryParse(soundId);
            if (id == null) {
                return;
            }

            var sound = Registries.SOUND_EVENT.get(id);
            if (sound == null || Registries.SOUND_EVENT.getId(sound) == null) {
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
        if (mc.player != null && mc.getSoundManager() != null) {
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
        if (mc.player != null && mc.getSoundManager() != null) {

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
