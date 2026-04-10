package me.valkeea.fishyaddons.vconfig.api;

import java.util.function.Consumer;

import me.valkeea.fishyaddons.vconfig.config.ConfigFile;

/**
 * Interface for enum keys.
 * <p>
 * @param <T> The type of value this key stores (Boolean, Integer, Double, or String)
 */
public sealed interface ConfigKey<T> permits BooleanKey, IntKey, DoubleKey, StringKey {
    /**
     * Get the JSON key string used for serialization.
     */
    String getString();
    
    /**
     * Get the config file this key belongs to.
     */
    ConfigFile getConfigFile();
    
    /**
     * Get the default value for this key.
     * @return The default value with the correct type
     */
    T getDefault();
    
    /**
     * Add a listener to be notified when this config value changes.
     * @param listener Consumer receiving the new value
     */
    void addListener(Consumer<T> listener);
    
    /**
     * Notify all listeners that this config value has changed.
     * Called internally by the Config manager.
     * @param newValue The new value
     */
    void notifyChange(T newValue);
    
    /**
     * Find a config key by its string representation across all typed enums.
     * Used for migration and dynamic lookup.
     * @param keyStr The JSON key string to search for
     * @return The matching ConfigKey, or null if not found
     */
    @SuppressWarnings("java:S1452") // Wildcard return type necessary for migration
    static ConfigKey<?> findByString(String oldStr) {

        String keyStr = toCamelCase(oldStr);
        for (var k : BooleanKey.values()) {
            if (k.getString().equals(keyStr)) return k;
        }
        for (var k : IntKey.values()) {
            if (k.getString().equals(keyStr)) return k;
        }
        for (var k : DoubleKey.values()) {
            if (k.getString().equals(keyStr)) return k;
        }
        for (var k : StringKey.values()) {
            if (k.getString().equals(keyStr)) return k;
        }
        return null;
    }

    private static String toCamelCase(String str) {
        String[] parts = str.split("_");
        if (parts.length < 2) return str;
        StringBuilder camel = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            camel.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1));
        }
        return camel.toString();
    }
}
