package me.valkeea.fishyaddons.vconfig.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.valkeea.fishyaddons.vconfig.config.ConfigFile;

public enum IntKey implements ConfigKey<Integer> {
    // Colors
    XP_COLOR("xpColor", 0xFFCFCFCF),
    TRANS_LAVA_COLOR("fishyTransLavaColor", -13700380),
    HUD_PETXP_COLOR("petXpColor", 0xFF67EA94),
    REDSTONE_COLOR_INDEX("customParticleColorIndex", 1),
    REDSTONE_COLOR("customParticleColor"),
    RENDER_COORD_COLOR("renderCoordsColor", -5653771),
    
    // Settings
    THEME_MODE("themeMode"),
    HUD_COLLECTION_LINES("collectionTrackerLines", 6),
    HUD_PROFIT_LINES("profitTrackerLines", 10),
    FWP_DISTANCE("waypointChainsDistance", 3),
    RENDER_COORD_MS("renderCoordsMs", 60000),

    NONE("NaN")
    ;
    
    private final String jsonKey;
    private final Integer defaultValue;
    private final ConfigFile configFile;
    private final List<Consumer<Integer>> listeners = new ArrayList<>();

    IntKey(String jsonKey) {
        this(jsonKey, 0, ConfigFile.SETTINGS);
    }
    
    IntKey(String jsonKey, Integer def) {
        this(jsonKey, def, ConfigFile.SETTINGS);
    }
    
    IntKey(String jsonKey, Integer def, ConfigFile configFile) {
        this.jsonKey = jsonKey;
        this.defaultValue = def;
        this.configFile = configFile;
    }
    
    @Override
    public String getString() {
        return jsonKey;
    }
    
    @Override
    public Integer getDefault() {
        return defaultValue != null ? defaultValue : 0;
    }
    
    @Override
    public ConfigFile getConfigFile() {
        return configFile;
    }
    
    @Override
    public void addListener(Consumer<Integer> listener) {
        listeners.add(listener);
    }
    
    @Override
    @SuppressWarnings("squid:S4276")    
    public void notifyChange(Integer newValue) {
        for (Consumer<Integer> listener : listeners) {
            listener.accept(newValue);
        }
    }
}
