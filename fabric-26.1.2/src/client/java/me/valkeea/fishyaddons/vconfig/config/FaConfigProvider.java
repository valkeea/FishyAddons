package me.valkeea.fishyaddons.vconfig.config;

import java.util.EnumMap;
import java.util.Map;

import me.valkeea.fishyaddons.vconfig.api.ConfigKey;
import me.valkeea.fishyaddons.vconfig.config.impl.AlertConfig;
import me.valkeea.fishyaddons.vconfig.config.impl.ColorConfig;
import me.valkeea.fishyaddons.vconfig.config.impl.FishyConfig;
import me.valkeea.fishyaddons.vconfig.config.impl.HudConfig;
import me.valkeea.fishyaddons.vconfig.config.impl.ItemConfig;
import me.valkeea.fishyaddons.vconfig.config.impl.ShortcutsConfig;
import me.valkeea.fishyaddons.vconfig.config.impl.StatConfig;

public class FaConfigProvider implements ConfigProvider {
    private final Map<ConfigFile, BaseConfig> configs = new EnumMap<>(ConfigFile.class);
    
    public FaConfigProvider() {
        init();
    }
    
    private void init() {
        configs.put(ConfigFile.SETTINGS, FishyConfig.getInstance());
        configs.put(ConfigFile.COLORS, ColorConfig.getInstance());
        configs.put(ConfigFile.HUD, HudConfig.getInstance());
        configs.put(ConfigFile.SHORTCUTS, ShortcutsConfig.getInstance());
        configs.put(ConfigFile.ALERTS, AlertConfig.getInstance());
        configs.put(ConfigFile.ITEMS, ItemConfig.getInstance());
        configs.put(ConfigFile.STATS, StatConfig.getInstance());
        
        configs.values().forEach(this::initConfig);

        FishyConfig.getInstance().removeDeprecatedFiles();
    }

    private void initConfig(BaseConfig config) {
        if (config != null) config.init();
    }

    /** Get the internal config map */
    public Map<ConfigFile, BaseConfig> getConfigs() {
        return configs;
    }
    
    /** Get the BaseConfig associated with the given key */
    private BaseConfig getConfig(ConfigKey<?> key) {
        BaseConfig bc = configs.get(key.getConfigFile());
        if (bc == null) {
            throw new IllegalStateException("Config not initialized for: " + key.getConfigFile());
        }
        return bc;
    }
    
    @Override
    public <T> T get(ConfigKey<T> key) {       
        return getConfig(key).getValue(key);
    }
    
    @Override
    public <T> void set(ConfigKey<T> key, T v) {
        BaseConfig bc = getConfig(key);
        bc.setValue(key, v);
    }
}
