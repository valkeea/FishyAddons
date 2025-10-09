package me.valkeea.fishyaddons.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import net.minecraft.client.MinecraftClient;

public class StatConfig {
    private StatConfig() {}

    private static final File CONFIG_FILE;
    private static final File BACKUP_DIR;
    private static final File BACKUP_FILE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();


    public static class Stats<V> {
        private final Map<String, V> values;
        private final String valuesKey;
        private final Type valueType;
        private final Consumer<Void> onChange;

        public Stats(String valuesKey, Type valueType, Consumer<Void> onChange) {
            this.values = new LinkedHashMap<>();
            this.valuesKey = valuesKey;
            this.valueType = valueType;
            this.onChange = onChange;
        }

        public Map<String, V> getValues() { return values; }

        public void set(String key, V value) {
            if (key == null || key.isEmpty()) return;
            values.put(key, value);
            if (onChange != null) onChange.accept(null);
        }

        public void remove(String key) {
            if (key == null || key.isEmpty()) return;
            values.remove(key);
            if (onChange != null) onChange.accept(null);
        }

        public void loadFromJson(JsonObject json) {
            if (json.has(valuesKey)) {
                Map<String, V> loaded = GSON.fromJson(json.get(valuesKey), valueType);
                values.clear();
                values.putAll(loaded);
            }
        }

        public void saveToJson(JsonObject json) {
            json.add(valuesKey, GSON.toJsonTree(values));
        }
    }

    private static final Stats<Integer> diana =
        new Stats<>("diana",
            new TypeToken<Map<String, Integer>>(){}.getType(),
            v -> save());

    private static final Stats<Integer> since =
        new Stats<>("since",
            new TypeToken<Map<String, Integer>>(){}.getType(),
            v -> save());

    private static final Stats<Integer> catchGraph =
        new Stats<>("catch_graph",
            new TypeToken<Map<String, Integer>>(){}.getType(),
            v -> save());

    private static final Stats<String> ignoredScs =
        new Stats<>("ignored_scs",
            new TypeToken<Map<String, String>>(){}.getType(),
            v -> save());        

    private static final Stats<String> sbUserSettings =
        new Stats<>("sb_user_settings",
            new TypeToken<Map<String, String>>(){}.getType(),
            v -> save());

    static {
        File root = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons");
        CONFIG_FILE = new File(root, "fishystats.json");
        BACKUP_DIR = new File(root, "backup");
        BACKUP_FILE = new File(BACKUP_DIR, "fishystats.json");
    }

    private static boolean recreatedConfig = false;
    private static boolean restoredConfig = false;

    public static boolean isRecreated() { return recreatedConfig; }
    public static boolean isRestored() { return restoredConfig; }

    public static void resetFlags() {
        recreatedConfig = false;
        restoredConfig = false;
    }

    public static void init() {
        CONFIG_FILE.getParentFile().mkdirs();
        BACKUP_DIR.mkdirs();
        load();
    }

    public static synchronized void load() {
        if (!CONFIG_FILE.exists()) {
            System.err.println("[StatConfig] Config file does not exist. Checking for backup...");
            loadOrRestore();
            return;
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            if (json == null || !validate(json)) {
                System.err.println("[StatConfig] Invalid config detected. Attempting restore...");
                loadOrRestore();
                return;
            }

            diana.loadFromJson(json);
            since.loadFromJson(json);
            catchGraph.loadFromJson(json);
            ignoredScs.loadFromJson(json);
            sbUserSettings.loadFromJson(json);

        } catch (JsonSyntaxException | JsonIOException e) {
            System.err.println("[StatConfig] Malformed JSON: " + e.getMessage());
            loadOrRestore();
        } catch (IOException e) {
            System.err.println("[StatConfig] Failed to read config: " + e.getMessage());
            loadOrRestore();
        }
    }

    public static synchronized void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            JsonObject json = new JsonObject();
            diana.saveToJson(json);
            since.saveToJson(json);
            catchGraph.saveToJson(json);
            ignoredScs.saveToJson(json);
            sbUserSettings.saveToJson(json);

            GSON.toJson(json, writer);
        } catch (IOException e) {
            System.err.println("[StatConfig] Failed to save config: " + e.getMessage());
        }
    }

    public static void saveBackup() {
        try {
            if (CONFIG_FILE.exists()) {
                Files.copy(CONFIG_FILE.toPath(), BACKUP_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[StatConfig] Failed to create backup: " + e.getMessage());
        }
    }

    private static boolean validate(JsonObject json) {
        return json != null;
    }

    private static void loadOrRestore() {
        if (BACKUP_FILE.exists()) {
            System.err.println("[StatConfig] Restoring from backup...");
            try {
                Files.copy(BACKUP_FILE.toPath(), CONFIG_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                load();
                restoredConfig = true;
                return;
            } catch (IOException e) {
                System.err.println("[StatConfig] Backup restore failed: " + e.getMessage());
            }
        }

        System.err.println("[StatConfig] No backup found. Creating default config...");
        save();
        recreatedConfig = true;
    }

    // ---  Diana Stats ---
    public static int getDiana(String key) {
        Integer v = diana.getValues().get(key);
        return v != null ? v : 0;
    }

    public static int getDiana(String key, int def) {
        Integer v = diana.getValues().getOrDefault(key, def);
        if (v == null) {
            diana.set(key, def);
            save();
            return def;
        }
        return v;
    }

    public static void setDiana(String key, int value) {
        diana.set(key, value);
        save();
    }

    public static Map<String, Integer> getAllDiana() {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<String, Integer> entry : diana.getValues().entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    // --- Area Counters ---
    public static Map<String, Integer> getAllSince() {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<String, Integer> entry : since.getValues().entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
    
    public static void setSince(String key, int value) {
        since.set(key, value);
        save();
    }

    public static int getSince(String key) {
        Integer v = since.getValues().get(key);
        return v != null ? v : 0;
    }

    // --- Catch Histogram ---
    public static int getcatchGraph(String key) {
        Integer v = catchGraph.getValues().get(key);
        return v != null ? v : 0;
    }

    public static int getData(String key, int def) {
        Integer v = catchGraph.getValues().getOrDefault(key, def);
        if (v == null) {
            catchGraph.set(key, def);
            save();
            return def;
        }
        return v;
    }

    public static void setData(String key, int value) {
        catchGraph.set(key, value);
        save();
    }

    public static void removeData(String key) {
        catchGraph.remove(key);
        save();
    }

    public static Map<String, Integer> getAllData() {
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<String, Integer> entry : catchGraph.getValues().entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    // --- Ignored Sea Creatures ---
    public static boolean isIgnoredSc(String key) {
        return ignoredScs.getValues().containsKey(key);
    }

    public static void setIgnoredSc(String key, boolean ignored) {
        if (ignored) {
            ignoredScs.set(key, "true");
        } else {
            ignoredScs.remove(key);
        }
        save();
    }

    public static Map<String, String> getAllIgnoredScs() {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, String> entry : ignoredScs.getValues().entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    // --- Skyblock User Settings ---
    public static String getChatMode() {
        return sbUserSettings.getValues().getOrDefault("current_mode", "ALL");
    }

    public static void setChatMode(String mode) {
        if (mode == null || mode.trim().isEmpty()) {
            mode = "ALL";
        }
        sbUserSettings.set("current_mode", mode.toUpperCase());
        save();
    }
}