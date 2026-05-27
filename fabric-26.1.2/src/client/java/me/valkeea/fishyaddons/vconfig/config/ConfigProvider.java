package me.valkeea.fishyaddons.vconfig.config;

import me.valkeea.fishyaddons.vconfig.api.ConfigKey;

public interface ConfigProvider {
    
    /**
     * Get a value with generic type.
     * @param key The configuration key
     * @param <T> The expected type
     * @return The value or default
     */
    <T> T get(ConfigKey<T> key);
    
    /**
     * Set a value.
     * @param key The configuration key
     * @param value The value to set
     */
    <T> void set(ConfigKey<T> key, T value);
}
