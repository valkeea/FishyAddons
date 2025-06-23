package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Set;

public class SkyblockCleaner {
    private static boolean hypeOn = false;
    private static boolean phantomOn = false;

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
    
    private boolean isHypeSound(Identifier soundId) {
        return HYPE_SOUNDS.contains(soundId);
    }

    private boolean isPhantomSound(Identifier soundId) {
        return PHANTOM_SOUNDS.contains(soundId);
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

    public static void refresh() {
        hypeOn = FishyConfig.getState("cleanHype", false);
        phantomOn = FishyConfig.getState("mutePhantom", false);
    }    
}