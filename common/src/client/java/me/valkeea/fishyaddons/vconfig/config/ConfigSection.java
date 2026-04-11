package me.valkeea.fishyaddons.vconfig.config;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import me.valkeea.fishyaddons.vconfig.api.ConfigKey;

/**
 * Each section manages a map of string keys to values of type V,
 * and optionally a map of toggle states (boolean) for each key.
 */
public class ConfigSection<V> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private final Map<String, V> values;
    private final @Nullable Map<String, Boolean> toggled;
    private final String valuesKey;
    private final @Nullable String toggledKey;
    private final Type valueType;
    private final Type toggledType;
    private final Consumer<Void> onChange;
    
    /**
     * Create a simple config section with values only.
     */
    public ConfigSection(String valuesKey, Type valueType, Consumer<Void> onChange) {
        this(valuesKey, null, valueType, null, onChange);
    }
    
    /**
     * Create a config section with values and toggle states.
     */
    public ConfigSection(String valuesKey, String toggledKey, Type valueType, Type toggledType, Consumer<Void> onChange) {
        this.values = new ConcurrentHashMap<>();
        this.toggled = toggledKey != null ? new ConcurrentHashMap<>() : null;
        this.valuesKey = valuesKey;
        this.toggledKey = toggledKey;
        this.valueType = valueType;
        this.toggledType = toggledType;
        this.onChange = onChange;
    }
    
    /**
     * Get all values in this section.
     */
    public Map<String, V> getValues() {
        return values;
    }
    
    /**
     * Get all toggle states (if this section supports toggles).
     */
    public Map<String, Boolean> getToggled() throws UnsupportedOperationException {
        if (toggled == null) {
            throw new UnsupportedOperationException("This section does not support toggles");
        }
        return toggled;
    }
    
    /**
     * Get a value with a default fallback.
     */
    public V get(String key, V defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }
    
    /**
     * Get a raw value.
     */
    public V get(String key) {
        return values.get(key);
    }
    
    /**
     * Get a value by typed key, or the key's default if not set.
     */
    public V get(ConfigKey<V> key) {
        V value = values.get(key.getString());
        return value != null ? value : key.getDefault();
    }
    
    /**
     * Set a value in this section.
     */
    public void set(String key, V value) {
        if (key == null || key.isEmpty()) return;
        values.put(key, value);
        if (toggled != null) {
            toggled.putIfAbsent(key, true);
        }
        if (onChange != null) onChange.accept(null);
    }
    
    /**
     * Set a value by typed key.
     */
    public void set(ConfigKey<V> key, V value) {
        set(key.getString(), value);
    }
    
    /**
     * Remove a value from this section.
     */
    public void remove(String key) {
        if (key == null || key.isEmpty()) return;
        values.remove(key);
        if (toggled != null) {
            toggled.remove(key);
        }
        if (onChange != null) onChange.accept(null);
    }
    
    /**
     * Check if a value is toggled on (if toggles are supported).
     */
    public boolean isToggled(String key) throws UnsupportedOperationException {
        if (toggled == null) {
            throw new UnsupportedOperationException("This section does not support toggles");
        }
        return toggled.getOrDefault(key, true);
    }
    
    /**
     * Set the toggle state for a value (if toggles are supported).
     */
    public void toggle(String key, boolean enabled) throws UnsupportedOperationException {
        if (toggled == null) {
            throw new UnsupportedOperationException("This section does not support toggles");
        }
        
        if (values.containsKey(key)) {
            toggled.put(key, enabled);
            if (onChange != null) onChange.accept(null);
        } else {
            toggled.put(key, enabled);
        }
    }
    
    /**
     * Load this section from JSON.
     */
    public void loadFromJson(JsonObject json) {
        if (json.has(valuesKey)) {
            Map<String, V> loaded = GSON.fromJson(json.get(valuesKey), valueType);
            if (loaded != null) {
                values.clear();
                values.putAll(loaded);
            }
        }
        
        if (toggled != null && toggledKey != null && json.has(toggledKey)) {
            Map<String, Boolean> loadedToggled = GSON.fromJson(json.get(toggledKey), toggledType);
            if (loadedToggled != null) {
                toggled.clear();
                toggled.putAll(loadedToggled);
            }
        }
    }
    
    /**
     * Save this section to JSON.
     */
    public void saveToJson(JsonObject json) {
        json.add(valuesKey, GSON.toJsonTree(values));
        if (toggled != null && toggledKey != null) {
            json.add(toggledKey, GSON.toJsonTree(toggled));
        }
    }
    
    /**
     * Check if this section has a value for the given key.
     */
    public boolean has(String key) {
        return values.containsKey(key);
    }
    
    /**
     * Get the number of values in this section.
     */
    public int size() {
        return values.size();
    }
    
    /**
     * Check if this section is empty.
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }
    
    /**
     * Clear all values (and toggles if applicable).
     */
    public void clear() {
        values.clear();
        if (toggled != null) {
            toggled.clear();
        }
        if (onChange != null) onChange.accept(null);
    }
}
