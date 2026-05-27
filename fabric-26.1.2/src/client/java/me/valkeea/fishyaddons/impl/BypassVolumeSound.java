package me.valkeea.fishyaddons.impl;

/**
 * Marker interface for sound instances.
 * When implemented, sounds will not be affected by master volume or category volume settings.
 */
public interface BypassVolumeSound {
    /**
     * @return true if this sound should bypass volume settings and play at its raw volume
     */
    boolean shouldBypassVolumeSettings();
}
