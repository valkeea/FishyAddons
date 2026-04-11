package me.valkeea.fishyaddons.vconfig.config.impl;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.vconfig.config.BaseConfig;
import me.valkeea.fishyaddons.vconfig.config.ConfigSection;

@SuppressWarnings("squid:S6548")
public class StatConfig extends BaseConfig {
    
    private static final StatConfig INSTANCE = new StatConfig();
    
    private final ConfigSection<Integer> diana =
        new ConfigSection<>("diana",
            new TypeToken<Map<String, Integer>>(){}.getType(),
            v -> requestSave());

    private final ConfigSection<Integer> since =
        new ConfigSection<>("since",
            new TypeToken<Map<String, Integer>>(){}.getType(),
            v -> requestSave());

    private final ConfigSection<Integer> catchGraph =
        new ConfigSection<>("catch_graph",
            new TypeToken<Map<String, Integer>>(){}.getType(),
            v -> requestSave());

    private final ConfigSection<String> ignoredScs =
        new ConfigSection<>("ignored_scs",
            new TypeToken<Map<String, String>>(){}.getType(),
            v -> requestSave());        

    private final ConfigSection<String> sbUserSettings =
        new ConfigSection<>("sb_user_settings",
            new TypeToken<Map<String, String>>(){}.getType(),
            v -> requestSave());

    private final ConfigSection<Integer> slayer =
        new ConfigSection<>("slayer",
            new TypeToken<Map<String, Integer>>(){}.getType(),
            v -> requestSave());

    private final ConfigSection<String> slayerStrings =
        new ConfigSection<>("slayer_strings",
            new TypeToken<Map<String, String>>(){}.getType(),
            v -> requestSave());
    
    private StatConfig() {
        super("fishystats.json");
    }
    
    public static StatConfig getInstance() {
        return INSTANCE;
    }
    
    @Override
    protected void loadFromJson(JsonObject json) {
        diana.loadFromJson(json);
        since.loadFromJson(json);
        catchGraph.loadFromJson(json);
        ignoredScs.loadFromJson(json);
        sbUserSettings.loadFromJson(json);
        slayer.loadFromJson(json);
        slayerStrings.loadFromJson(json);
    }
    
    @Override
    protected void saveToJson(JsonObject json) {
        diana.saveToJson(json);
        since.saveToJson(json);
        catchGraph.saveToJson(json);
        ignoredScs.saveToJson(json);
        sbUserSettings.saveToJson(json);
        slayer.saveToJson(json);
        slayerStrings.saveToJson(json);
    }
    
    // --- Batch Operations ---
    
    public static void beginBatch() {
        INSTANCE.incrementDepth();
    }
    
    public static void endBatch() {
        INSTANCE.decrementDepthAndFlush();
    }

    // --- Diana Stats ---
    
    public static int getDiana(String key) {
        Integer v = INSTANCE.diana.get(key);
        return v != null ? v : 0;
    }

    public static int getDiana(String key, int def) {
        return INSTANCE.diana.get(key, def);
    }

    public static void setDiana(String key, int value) {
        INSTANCE.diana.set(key, value);
    }

    public static Map<String, Integer> getAllDiana() {
        return new HashMap<>(INSTANCE.diana.getValues());
    }

    // --- Area Counters (Since) ---
    
    public static Map<String, Integer> getAllSince() {
        return new HashMap<>(INSTANCE.since.getValues());
    }
    
    public static void setSince(String key, int value) {
        INSTANCE.since.set(key, value);
    }

    public static int getSince(String key) {
        Integer v = INSTANCE.since.get(key);
        return v != null ? v : 0;
    }

    // --- Slayer Stats ---

    public static int getSlayer(String key, int def) {
        return INSTANCE.slayer.get(key, def);
    }

    public static void setSlayer(String key, int value) {
        INSTANCE.slayer.set(key, value);
    }

    public static String getSlayerString(String key, String def) {
        return INSTANCE.slayerStrings.get(key, def);
    }

    public static void setSlayerString(String key, String value) {
        INSTANCE.slayerStrings.set(key, value);
    }

    // --- Catch Histogram (Data) ---
    
    public static int getcatchGraph(String key) {
        Integer v = INSTANCE.catchGraph.get(key);
        return v != null ? v : 0;
    }

    public static int getData(String key, int def) {
        return INSTANCE.catchGraph.get(key, def);
    }

    public static void setData(String key, int value) {
        INSTANCE.catchGraph.set(key, value);
    }

    public static void removeData(String key) {
        INSTANCE.catchGraph.remove(key);
    }

    public static Map<String, Integer> getAllData() {
        return new HashMap<>(INSTANCE.catchGraph.getValues());
    }

    // --- Ignored Sea Creatures ---
    
    public static boolean isIgnoredSc(String key) {
        return INSTANCE.ignoredScs.getValues().containsKey(key);
    }

    public static void setIgnoredSc(String key, boolean ignored) {
        if (ignored) {
            INSTANCE.ignoredScs.set(key, "true");
        } else {
            INSTANCE.ignoredScs.remove(key);
        }
    }

    public static Map<String, String> getAllIgnoredScs() {
        return new HashMap<>(INSTANCE.ignoredScs.getValues());
    }

    // --- Skyblock User Settings ---
    
    public static String getChatMode() {
        return INSTANCE.sbUserSettings.get("current_mode", "ALL");
    }

    public static void setChatMode(String mode) {
        if (mode == null || mode.trim().isEmpty()) {
            mode = "ALL";
        }
        INSTANCE.sbUserSettings.set("current_mode", mode.toUpperCase());
    }
}
