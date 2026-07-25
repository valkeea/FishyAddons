package me.valkeea.fishyaddons.impl;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Custom sound instance for control over sound playback
 */
public class MutableSoundInstance implements SoundInstance, BypassVolumeSound {
    private final SoundEvent soundEvent;
    private final SoundSource category;
    private final float volume;
    private final float pitch;
    private final boolean repeatable;
    private final int repeatDelay;
    private final Attenuation attenuationType;
    private final double x;
    private final double y; 
    private final double z;
    private final boolean relative;
    private final RandomSource random;
    private final boolean bypassVolumeSettings;
    private Sound sound;

    private MutableSoundInstance(Builder builder) {
        this.soundEvent = builder.soundEvent;
        this.category = builder.category;
        this.volume = builder.volume;
        this.pitch = builder.pitch;
        this.repeatable = builder.repeatable;
        this.repeatDelay = builder.repeatDelay;
        this.attenuationType = builder.attenuationType;
        this.x = builder.x;
        this.y = builder.y;
        this.z = builder.z;
        this.relative = builder.relative;
        this.random = builder.random;
        this.bypassVolumeSettings = builder.bypassVolumeSettings;
    }

    @Override
    @Nullable
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        WeighedSoundEvents soundSet = soundManager.getSoundEvent(getIdentifier());
        if (soundSet == null) {
            this.sound = SoundManager.EMPTY_SOUND;
        } else {
            this.sound = soundSet.getSound(random);
        }
        return soundSet;
    }

    @Override
    public Sound getSound() {
        return sound != null ? sound : SoundManager.EMPTY_SOUND;
    }

    @Override
    public Attenuation getAttenuation() {
        return attenuationType;
    }

    @Override public Identifier getIdentifier() { return soundEvent.location(); }
    @Override public SoundSource getSource() { return category; }
    @Override public boolean isLooping() { return repeatable; }
    @Override public boolean isRelative() { return relative; }
    @Override public int getDelay() { return repeatDelay; }
    @Override public float getVolume() { return volume; }
    @Override public float getPitch() { return pitch; }
    @Override public double getX() { return x; }
    @Override public double getY() { return y; }
    @Override public double getZ() { return z; }

    @Override
    public boolean shouldBypassVolumeSettings() {
        return bypassVolumeSettings;
    }    

    public static class Builder {
        private SoundEvent soundEvent;
        private SoundSource category = SoundSource.MASTER;
        private float volume = 1.0F;
        private float pitch = 1.0F;
        private boolean repeatable = false;
        private int repeatDelay = 0;
        private Attenuation attenuationType = Attenuation.NONE;
        private double x = 0.0;
        private double y = 0.0;
        private double z = 0.0;
        private boolean relative = true;
        private RandomSource random = RandomSource.create();
        private boolean bypassVolumeSettings = false;

        public Builder(SoundEvent soundEvent) {
            this.soundEvent = soundEvent;
        }

        public Builder category(SoundSource category) {
            this.category = category;
            return this;
        }

        public Builder volume(float volume) {
            this.volume = volume;
            return this;
        }

        public Builder pitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        public Builder repeatable(boolean repeatable) {
            this.repeatable = repeatable;
            return this;
        }

        public Builder repeatDelay(int repeatDelay) {
            this.repeatDelay = repeatDelay;
            return this;
        }

        public Builder attenuationType(Attenuation attenuationType) {
            this.attenuationType = attenuationType;
            return this;
        }

        public Builder position(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.relative = false;
            return this;
        }

        public Builder relative(boolean relative) {
            this.relative = relative;
            return this;
        }

        public Builder noRandom(boolean noRandom) {
            if (noRandom) this.random.setSeed(0);
            return this;
        }

        public Builder bypassVolumeSettings(boolean bypass) {
            this.bypassVolumeSettings = bypass;
            return this;
        }

        public MutableSoundInstance build() {
            return new MutableSoundInstance(this);
        }
    }

    public static MutableSoundInstance master(SoundEvent soundEvent, float pitch, float volume, boolean noRandom) {
        return new Builder(soundEvent)
                .pitch(pitch)
                .volume(volume)
                .noRandom(noRandom)
                .build();
    }

    public static MutableSoundInstance ambient(SoundEvent soundEvent, float pitch, float volume) {
        return new Builder(soundEvent)
                .category(SoundSource.AMBIENT)
                .pitch(pitch)
                .volume(volume)
                .build();
    }

    public static MutableSoundInstance repeating(SoundEvent soundEvent, float pitch, float volume, int repeatDelay) {
        return new Builder(soundEvent)
                .category(SoundSource.MASTER)
                .pitch(pitch)
                .volume(volume)
                .repeatable(true)
                .repeatDelay(repeatDelay)
                .build();
    }

    public static MutableSoundInstance masterBypass(SoundEvent soundEvent, float pitch, float volume, boolean noRandom) {
        return new Builder(soundEvent)
                .pitch(pitch)
                .volume(volume)
                .bypassVolumeSettings(true)
                .noRandom(noRandom)
                .build();
    }
}
