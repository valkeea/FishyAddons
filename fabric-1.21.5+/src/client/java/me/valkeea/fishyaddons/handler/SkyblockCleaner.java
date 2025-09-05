package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Set;

public class SkyblockCleaner {
    private static boolean hypeOn = false;
    private static boolean phantomOn = false;
    private static boolean runeOn = false;
    private static boolean hideHotspot = false;
    private static float hotspotDistance = 0.0f;

    private static final Set<Identifier> HYPE_SOUNDS = Set.of(
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_GENERIC_EXPLODE.value()),
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE),
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_ENDERMAN_TELEPORT)
    );

    private static final Set<Identifier> PHANTOM_SOUNDS = Set.of(
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_PHANTOM_AMBIENT),
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_PHANTOM_BITE),
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_PHANTOM_DEATH),
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_PHANTOM_FLAP),
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_PHANTOM_HURT),
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_PHANTOM_SWOOP)
    );

    public static boolean shouldClean(Identifier soundId) {
        return shouldCleanHype(soundId) || shouldMutePhantom(soundId) || shouldMuteRune(soundId);
    }

    public static boolean shouldCleanHype(Identifier soundId) {
        return hypeOn && HYPE_SOUNDS.contains(soundId);
    }

    public static boolean shouldCleanHype() {
        return hypeOn;
    }

    public static boolean shouldMutePhantom(Identifier soundId) {
        return phantomOn && PHANTOM_SOUNDS.contains(soundId);
    }

    public static boolean shouldMuteRune(Identifier soundId) {
        return runeOn && isRuneSound(soundId);
    }

    public static boolean isRuneSound(Identifier soundId) {
        return soundId.getPath().contains("entity.cat.ambient") || 
               soundId.getPath().contains("entity.villager.ambient") || 
               soundId.getPath().contains("entity.wolf.ambient") ||
               soundId.getPath().contains("entity.wolf.growl");
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
        hideHotspot = FishyConfig.getState(Key.HIDE_HOTSPOT, false);
        hotspotDistance = FishyConfig.getFloat(Key.HOTSPOT_DISTANCE, 7.0f);
    }    

    private SkyblockCleaner() {
        throw new UnsupportedOperationException("Utility class");
    }
}