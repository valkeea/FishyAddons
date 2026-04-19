package me.valkeea.fishyaddons.vconfig.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.valkeea.fishyaddons.vconfig.config.ConfigFile;

public enum StringKey implements ConfigKey<String> {

    KEY_LOCK_SLOT("lockKey", "NONE"),
    PRICE_TYPE("priceType", "sellPrice"),
    KEY_HIDE_GUI("lockGuiSlotKey", "NONE"),
    INFO_ID("infoId"),
    REEL_OVERRIDE_ID("reelAlert", "block.note_block.pling"),
    FERO_OVERRIDE_ID("feroAlert", "entity.zombie.break_wooden_door"),
    ITEM_CONFIG("heldItemConfigData"),
    EXCLUDED_ITEMS("trackerExcludedItems"),
    EXCLUDED_COLLECTIONS("hiddenCollections"),
    COLLECTION_GOAL("activeGoal"),
    BLACKLIST_EXCEPTIONS("blacklistExceptions"),

    NONE("NaN")
    ;
    
    private final String jsonKey;
    private final String defaultValue;
    private final ConfigFile configFile;
    private final List<Consumer<String>> listeners = new ArrayList<>();

    StringKey(String jsonKey) {
        this(jsonKey, "", ConfigFile.SETTINGS);
    }
    
    StringKey(String jsonKey, String def) {
        this(jsonKey, def, ConfigFile.SETTINGS);
    }
    
    StringKey(String jsonKey, String def, ConfigFile configFile) {
        this.jsonKey = jsonKey;
        this.defaultValue = def;
        this.configFile = configFile;
    }
    
    @Override
    public String getString() {
        return jsonKey;
    }
    
    @Override
    public String getDefault() {
        return defaultValue != null ? defaultValue : "";
    }
    
    @Override
    public ConfigFile getConfigFile() {
        return configFile;
    }
    
    @Override
    public void addListener(Consumer<String> listener) {
        listeners.add(listener);
    }
    
    @Override
    public void notifyChange(String newValue) {
        for (Consumer<String> listener : listeners) {
            listener.accept(newValue);
        }
    }
}
