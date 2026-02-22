package me.valkeea.fishyaddons.feature.skyblock;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.impl.MutableSoundInstance;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class SkyblockCleaner {
    private static boolean hypeOn = false;
    private static boolean phantomOn = false;
    private static boolean runeOn = false;
    private static boolean thunderOn = false;
    private static boolean hideHotspot = false;
    private static boolean feroTrueVol = false;    
    private static float hotspotDistance = 0.0f;
    private static float feroOn = 0.0f;    
    private static String feroPath = "";

    private static final String FERO = "entity.zombie.break_wooden_door";

    private static final Set<Identifier> HYPE_SOUNDS = Set.of(
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_GENERIC_EXPLODE.value()),
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE),
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_ENDERMAN_TELEPORT)
    );

    public static boolean runeSound(String path) {
        return path.contains("entity.cat.ambient") || 
               path.contains("entity.cat.purreow") ||
               path.contains("entity.villager.ambient") || 
               path.contains("entity.wolf.ambient") ||
               path.contains("entity.wolf.growl");
    }

    private static boolean thunderSound(String path) {
        return path.contains("lightning_bolt") ||
                path.contains("guardian");
    }

    /**
     * Check if a sound should be replaced with a different sound.
     * Returns a replacement SoundInstance if applicable, null otherwise.
     */
    @Nullable
    public static SoundInstance getReplacementSound(SoundInstance sound) {
        if (sound == null || !GameMode.skyblock()) return null;

        try {
            var soundId = sound.getId();
            if (soundId == null) return null;

            var fishingReplacement = CatchAlert.getReplacementSound(soundId);
            if (fishingReplacement != null) return fishingReplacement;

            var path = soundId.getPath();
            return getFeroReplacement(path, sound);

        } catch (Exception e) {
            // Ignore
            return null;
        }
    }

    /**
     * Check if a sound should be muted
     */
    public static boolean shouldClean(SoundInstance sound) {
        if (sound == null || !GameMode.skyblock()) return false;

        try {
            var soundId = sound.getId();
            if (soundId == null) return false;

            var path = soundId.getPath();
            return muteHype(soundId) || mutePhantom(path) ||
                    muteRune(path) || muteThunder(path);

        } catch (Exception e) {
            // Ignore
            return false;
        }
    }

    public static boolean muteHype(Identifier soundId) {
        return hypeOn && HYPE_SOUNDS.contains(soundId);
    }

    public static boolean mutePhantom(String path) {
        return phantomOn && path.contains("entity.phantom");
    }

    public static boolean muteRune(String path) {
        return runeOn && runeSound(path);
    }

    private static long thunderCalled = 0;

    private static boolean muteThunder(String path) {
        if (thunderOn && thunderSound(path)) {

            boolean thunderAlive = ValuableMobs.isMobAlive("Thunder");
            
            if (thunderAlive) {
                thunderCalled = System.currentTimeMillis();
                return true;

            } else if (System.currentTimeMillis() - thunderCalled < 65000) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private static SoundInstance getFeroReplacement(String path, SoundInstance original) {
        if (feroOn <= 0.0f || !path.contains(FERO)) return null;

        var id = Identifier.tryParse(feroPath);
        if (id == null) return null;

        var sound = Registries.SOUND_EVENT.get(id);
        if (sound == null || Registries.SOUND_EVENT.getId(sound) == null) return null;

        return feroTrueVol 
            ? MutableSoundInstance.masterBypass(sound, 1.0f, feroOn, false)
            : MutableSoundInstance.master(sound, 1.0f, feroOn, false);
    }     

    public static boolean shouldCleanHype() {
        return hypeOn;
    }    

    public static boolean shouldHideHotspot() {
        return hideHotspot;
    }

    public static float getHotspotDistance() {
        return hotspotDistance;
    }

    public static void refresh() {
        hypeOn = FishyConfig.getState(Key.CLEAN_HYPE, false);
        phantomOn = FishyConfig.getState(Key.MUTE_PHANTOM, false);
        runeOn = FishyConfig.getState(Key.MUTE_RUNE, false);
        thunderOn = FishyConfig.getState(Key.MUTE_THUNDER, false);
        hideHotspot = FishyConfig.getState(Key.HIDE_HOTSPOT, false);
        feroTrueVol = FishyConfig.getState(Key.FERO_TRUE_VOLUME, false);
        hotspotDistance = FishyConfig.getFloat(Key.HOTSPOT_DISTANCE, 7.0f);
        feroOn = FishyConfig.getFloat(Key.CUSTOM_FERO, 0.0f);
        feroPath = FishyConfig.getString(Key.FERO_ALERT, FERO);
    }    

    private SkyblockCleaner() {
        throw new UnsupportedOperationException("Utility class");
    }
}
