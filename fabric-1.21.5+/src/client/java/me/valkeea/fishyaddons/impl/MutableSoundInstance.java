package me.valkeea.fishyaddons.impl;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/**
 * Custom sound instance for control over sound playback
 */
public class MutableSoundInstance implements SoundInstance {
    private final SoundEvent soundEvent;
    private final SoundCategory category;
    private final float volume;
    private final float pitch;
    private final boolean repeatable;
    private final int repeatDelay;
    private final AttenuationType attenuationType;
    private final double x;
    private final double y; 
    private final double z;
    private final boolean relative;
    private final Random random;
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
        this.random = Random.create();
    }

    @Override
    @Nullable
    public WeightedSoundSet getSoundSet(SoundManager soundManager) {
        WeightedSoundSet soundSet = soundManager.get(getId());
        if (soundSet == null) {
            this.sound = SoundManager.MISSING_SOUND;
        } else {
            this.sound = soundSet.getSound(random);
        }
        return soundSet;
    }

    @Override
    public Sound getSound() {
        return sound != null ? sound : SoundManager.MISSING_SOUND;
    }

    @Override
    public AttenuationType getAttenuationType() {
        return attenuationType;
    }

    @Override public Identifier getId() { return soundEvent.id(); }
    @Override public SoundCategory getCategory() { return category; }
    @Override public boolean isRepeatable() { return repeatable; }
    @Override public boolean isRelative() { return relative; }
    @Override public int getRepeatDelay() { return repeatDelay; }
    @Override public float getVolume() { return volume; }
    @Override public float getPitch() { return pitch; }
    @Override public double getX() { return x; }
    @Override public double getY() { return y; }
    @Override public double getZ() { return z; }    

    public static class Builder {
        private SoundEvent soundEvent;
        private SoundCategory category = SoundCategory.MASTER;
        private float volume = 1.0F;
        private float pitch = 1.0F;
        private boolean repeatable = false;
        private int repeatDelay = 0;
        private AttenuationType attenuationType = AttenuationType.NONE;
        private double x = 0.0;
        private double y = 0.0;
        private double z = 0.0;
        private boolean relative = true;

        public Builder(SoundEvent soundEvent) {
            this.soundEvent = soundEvent;
        }

        public Builder category(SoundCategory category) {
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

        public Builder attenuationType(AttenuationType attenuationType) {
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

        public MutableSoundInstance build() {
            return new MutableSoundInstance(this);
        }
    }

    public static MutableSoundInstance master(SoundEvent soundEvent, float pitch, float volume) {
        return new Builder(soundEvent)
                .pitch(pitch)
                .volume(volume)
                .build();
    }

    public static MutableSoundInstance ambient(SoundEvent soundEvent, float pitch, float volume) {
        return new Builder(soundEvent)
                .category(SoundCategory.AMBIENT)
                .pitch(pitch)
                .volume(volume)
                .build();
    }

    public static MutableSoundInstance repeating(SoundEvent soundEvent, float pitch, float volume, int repeatDelay) {
        return new Builder(soundEvent)
                .category(SoundCategory.MASTER)
                .pitch(pitch)
                .volume(volume)
                .repeatable(true)
                .repeatDelay(repeatDelay)
                .build();
    }
}