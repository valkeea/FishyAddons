package me.valkeea.fishyaddons.vconfig.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.valkeea.fishyaddons.vconfig.config.ConfigFile;

public enum DoubleKey implements ConfigKey<Double> {

    DMG_SCALE("dmgScale", 0.15),
    MOD_UI_SCALE("modUiScale", 0.8),
    FILTER_MIN_VALUE("minItemValue", 2.0),
    INV_SEARCH_OPACITY("searchOverlayOpacity", 0.8),
    REEL_OVERRIDE("customReel"),
    FERO_OVERRIDE("customFero"),
    HOTSPOT_DISTANCE("hotspotDistance", 7.0),

    NONE("NaN")
    ;
    
    private final String jsonKey;
    private final Double defaultValue;
    private final ConfigFile configFile;
    private final List<Consumer<Double>> listeners = new ArrayList<>();

    DoubleKey(String jsonKey) {
        this(jsonKey, 0.0, ConfigFile.SETTINGS);
    }
    
    DoubleKey(String jsonKey, Double def) {
        this(jsonKey, def, ConfigFile.SETTINGS);
    }
    
    DoubleKey(String jsonKey, Double def, ConfigFile configFile) {
        this.jsonKey = jsonKey;
        this.defaultValue = def;
        this.configFile = configFile;
    }
    
    @Override
    public String getString() {
        return jsonKey;
    }
    
    @Override
    public Double getDefault() {
        return defaultValue != null ? defaultValue : 0.0;
    }
    
    @Override
    public ConfigFile getConfigFile() {
        return configFile;
    }
    
    @Override
    public void addListener(Consumer<Double> listener) {
        listeners.add(listener);
    }
    
    // Performance not an issue here, this is cleaner
    @SuppressWarnings("squid:S4276")
    @Override    
    public void notifyChange(Double newValue) {
        for (Consumer<Double> listener : listeners) {
            listener.accept(newValue);
        }
    }
}
