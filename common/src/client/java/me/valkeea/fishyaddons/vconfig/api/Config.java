package me.valkeea.fishyaddons.vconfig.api;

import java.util.Map;

import me.valkeea.fishyaddons.vconfig.config.BaseConfig;
import me.valkeea.fishyaddons.vconfig.config.ConfigFile;
import me.valkeea.fishyaddons.vconfig.config.ConfigProvider;
import me.valkeea.fishyaddons.vconfig.config.FaConfigProvider;

/**
 * Unified configuration facade for all files and keys.
 */
public class Config {
    private Config() {}
    
    private static ConfigProvider provider;
    private static boolean initialized = false;
    
    public static void setProvider(ConfigProvider newProvider) {
        provider = newProvider;
        initialized = true;
    }
    
    /**
     * Get the current config provider.
     * Lazily initializes the production provider if not already set.
     */
    public static ConfigProvider getProvider() {
        if (!initialized) {
            provider = new FaConfigProvider();
            initialized = true;
        }
        return provider;
    }
    
    private static void ensureInitialized() {
        if (!initialized) {
            getProvider();
        }
    }
    
    /** Get a value by key or its default */
    public static <T> T getValue(ConfigKey<T> key) {
        ensureInitialized();
        return provider.get(key);
    }
    
    /** Set a value by key */
    public static <T> void setValue(ConfigKey<T> key, T v) {
        ensureInitialized();
        provider.set(key, v);
        key.notifyChange(v);
    }
    
    // --- Primitive-specialized getters ---
    
    /** Get a boolean value by key or default false */    
    public static boolean get(BooleanKey key) {
        return getValue(key);
    }

    /** Get an int value by key or default 0 */
    public static int get(IntKey key) {
        return getValue(key);
    }

    /** Get a double value by key or default 0.0 */
    public static double get(DoubleKey key) {
        return getValue(key);
    }

    /** Get a String value by key or default "" */
    public static String get(StringKey key) {
        return getValue(key);
    }

    // --- Specialized setters ---
    
    public static void set(BooleanKey key, boolean v) {
        setValue(key, v);
    }
    
    public static void set(IntKey key, int v) {
        setValue(key, v);
    }
    
    public static void set(DoubleKey key, double v) {
        setValue(key, v);
    }
    
    public static void set(StringKey key, String v) {
        setValue(key, v);
    }    

    /** Toggle a boolean value by key */
    public static void toggle(BooleanKey key) {
        boolean current = get(key);
        setValue(key, !current);
    }
    
    // --- Utility ---
    
    public static void init() {
        ensureInitialized();
        if (provider instanceof FaConfigProvider) {
            provider = new FaConfigProvider();
        }
    }

    // --- Production ---

    /**
     * Returns an unmodifiable copy of the internal config map.
     */
    public static Map<ConfigFile, BaseConfig> getConfigs() {
        ensureInitialized();
        if (provider instanceof FaConfigProvider prod) {
            return Map.copyOf(prod.getConfigs());
        }
        return Map.of();
    }

    public static void resetFlags() {
        ensureInitialized();
        if (provider instanceof FaConfigProvider prod) {
            for (BaseConfig bc : prod.getConfigs().values()) {
                if (bc != null) {
                    bc.resetFlags();
                }
            }
        }
    }

    /**
     * Back up all configs, then reset every typed key to its default value.
     * Custom {@link ConfigSection}s are left untouched.
     * 
     * @return the number of keys reset
     */
    public static int resetAllToDefaults() {
        ensureInitialized();
        saveBackup();

        var configs = getConfigs().values();
        configs.forEach(BaseConfig::incrementDepth);
        int count = 0;
        try {
            for (var k : BooleanKey.values()) count += reset(k);
            for (var k : IntKey.values()) count += reset(k);
            for (var k : DoubleKey.values()) count += reset(k);
            for (var k : StringKey.values()) count += reset(k);
        } finally {
            configs.forEach(BaseConfig::decrementDepthAndFlush);
        }
        return count;
    }

    private static <T> int reset(ConfigKey<T> key) {
        try {
            setValue(key, key.getDefault());
            return 1;
        } catch (UnsupportedOperationException | IllegalStateException _) {
            return 0;
        }
    }

    public static void saveBackup() {
        ensureInitialized();
        if (provider instanceof FaConfigProvider prod) {
            for (BaseConfig bc : prod.getConfigs().values()) {
                if (bc != null) {
                    bc.saveBackup();
                }
            }
        }
    }
}
