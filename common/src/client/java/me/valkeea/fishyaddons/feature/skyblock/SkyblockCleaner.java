package me.valkeea.fishyaddons.feature.skyblock;

import java.util.Set;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public class SkyblockCleaner {
    private static boolean hypeOn = false;
    private static boolean phantomOn = false;
    private static boolean runeOn = false;
    private static boolean thunderOn = false;
    private static boolean hideHotspot = false;
    private static float hotspotDistance = 0.0f;

    private static final Set<Identifier> HYPE_SOUNDS = Set.of(
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_GENERIC_EXPLODE.value()),
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE),
        Registries.SOUND_EVENT.getId(SoundEvents.ENTITY_ENDERMAN_TELEPORT)
    );

    public static boolean isRuneSound(Identifier soundId) {
        return soundId.getPath().contains("entity.cat.ambient") || 
               soundId.getPath().contains("entity.cat.purreow") ||
               soundId.getPath().contains("entity.villager.ambient") || 
               soundId.getPath().contains("entity.wolf.ambient") ||
               soundId.getPath().contains("entity.wolf.growl");
    }

    private static boolean isThunderSound(Identifier soundId) {
        return soundId.getPath().contains("lightning_bolt") ||
                soundId.getPath().contains("guardian");
    }    

    public static boolean shouldClean(Identifier soundId) {
        if (!GameMode.skyblock()) return false;
        return shouldCleanHype(soundId) || shouldMutePhantom(soundId) ||
                shouldMuteRune(soundId) || shouldMuteThunder(soundId);
    }

    public static boolean shouldCleanHype(Identifier soundId) {
        return hypeOn && HYPE_SOUNDS.contains(soundId);
    }

    public static boolean shouldCleanHype() {
        return hypeOn;
    }

    public static boolean shouldMutePhantom(Identifier soundId) {
        return phantomOn && soundId.getPath().contains("entity.phantom");
    }

    public static boolean shouldMuteRune(Identifier soundId) {
        return runeOn && isRuneSound(soundId);
    }

    private static long thunderCalled = 0;

    private static boolean shouldMuteThunder(Identifier soundId) {
        if (thunderOn && isThunderSound(soundId)) {

            boolean thunderAlive = ValuableMobs.isMobAlive("Thunder");
            
            if (thunderAlive) {
                thunderCalled = System.currentTimeMillis();
                return true;

            } else if (System.currentTimeMillis() - thunderCalled < 60000) {
                return true;
            }
        }

        return false;
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
        hotspotDistance = FishyConfig.getFloat(Key.HOTSPOT_DISTANCE, 7.0f);
    }    

    private SkyblockCleaner() {
        throw new UnsupportedOperationException("Utility class");
    }
}
