package me.valkeea.fishyaddons.vconfig.config.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.listener.ClientConnected;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.ConfigKey;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import me.valkeea.fishyaddons.vconfig.config.BaseConfig;
import me.valkeea.fishyaddons.vconfig.config.ConfigSection;
import net.minecraft.client.MinecraftClient;

@SuppressWarnings("squid:S6548")
public class FishyConfig extends BaseConfig {

    private static final FishyConfig INSTANCE = new FishyConfig();
    private static final String BOOLEANS_KEY = "booleans";
    private static final String INTEGERS_KEY = "integers";
    private static final String DOUBLES_KEY = "doubles";
    private static final String STRINGS_KEY = "strings";
    private static final String LEGACY_SETTINGS_KEY = "settings";
    private static final String OLD_FA_FILE = "fishyaddons";
    private static final String OLD_ITEMS_FILE = "fishyitems";
    
    private final ConfigSection<Boolean> booleans = 
        new ConfigSection<>(BOOLEANS_KEY,
            new TypeToken<Map<String, Boolean>>(){}.getType(),
            v -> requestSave());
    
    private final ConfigSection<Integer> integers = 
        new ConfigSection<>(INTEGERS_KEY,
            new TypeToken<Map<String, Integer>>(){}.getType(),
            v -> requestSave());
    
    private final ConfigSection<Double> doubles = 
        new ConfigSection<>(DOUBLES_KEY,
            new TypeToken<Map<String, Double>>(){}.getType(),
            v -> requestSave());
    
    private final ConfigSection<String> strings = 
        new ConfigSection<>(STRINGS_KEY,
            new TypeToken<Map<String, String>>(){}.getType(),
            v -> requestSave());
    
    private FishyConfig() {
        super("settings.json");
    }
    
    public static FishyConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Automatic routing based on key type using pattern matching.
     * Zero overhead - compiles to a simple switch on enum type.
     */
    @Override
    @SuppressWarnings("unchecked")
    protected <T> ConfigSection<T> getSectionForKey(ConfigKey<T> key) {
        return switch (key) {
            case BooleanKey bk -> (ConfigSection<T>) booleans;
            case IntKey ik -> (ConfigSection<T>) integers;
            case DoubleKey dk -> (ConfigSection<T>) doubles;
            case StringKey sk -> (ConfigSection<T>) strings;
        };
    }
    
    @Override
    protected void loadFromJson(JsonObject json) {
        booleans.loadFromJson(json);
        integers.loadFromJson(json);
        doubles.loadFromJson(json);
        strings.loadFromJson(json);
    }
    
    @Override
    protected void saveToJson(JsonObject json) {
        booleans.saveToJson(json);
        integers.saveToJson(json);
        doubles.saveToJson(json);
        strings.saveToJson(json);
    }
    
    /**
     * Migration from old monolithic approach:
     * 1. fishyaddons.json (settings + merge modKeys sections)
     * 2. fishyitems.json (settings)
     * 3. Old mixed-type "settings" section in current file
     */
    @Override
    protected boolean tryMigrate() {

        var mc = MinecraftClient.getInstance();
        var faFile = new File(mc.runDirectory, "config/fishyaddons/fishyaddons.json");
        var itemFile = new File(mc.runDirectory, "config/fishyaddons/fishyitems.json");
        
        boolean migratedSettings = false;
        boolean migratedItems = false;
        
        if (isMigrationComplete(OLD_FA_FILE) && isMigrationComplete(OLD_ITEMS_FILE)) return true;

        boolean hasFaFile = faFile.exists();
        boolean hasItemFile = itemFile.exists();

        if (hasFaFile) backupOldConfigFile(faFile);
        if (hasItemFile) backupOldConfigFile(itemFile);
        
        incrementDepth();

        try {

            migratedSettings = tryMigrateSettings(faFile, OLD_FA_FILE, hasFaFile);
            migratedItems = tryMigrateSettings(itemFile, OLD_ITEMS_FILE, hasItemFile);
            
            if (configFile.exists()) {
                try (Reader reader = new FileReader(configFile)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json != null && json.has(LEGACY_SETTINGS_KEY)) {
                        log("Found old mixed-type 'settings' section, migrating to typed sections...");
                        migrateFromMixedSection(json);
                    }
                } catch (Exception e) {
                    logError("Migration from mixed section failed: " + e.getMessage());
                }
            }

            integers.set(VERSION_KEY, CURRENT_VERSION);
            
        } finally {
            decrementDepthAndFlush();
        }
        
        boolean migrated = migratedSettings || migratedItems;

        if (!migrated && (hasFaFile || hasItemFile)) {
            warn("Migration incomplete - migratedSettings=" + migratedSettings + ", migratedItems=" + migratedItems);
            ClientConnected.notifyMigrationIssues();
        } else if (migrated) {
            log("Migration completed successfully");
            requestSave();
        }
        
        return migrated;
    }

    private boolean tryMigrateSettings(File file, String sourceName, boolean hasFile) {
        boolean success = false;
        if (hasFile && !isMigrationComplete(sourceName)) {
            log("Attempting to migrate from " + sourceName + ".json...");
            try (Reader reader = new FileReader(file)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null && needsMigration(json)) {
                    success = migrateSettingsFromJson(json, sourceName + ".json");
                    if (success) {
                        markMigrationComplete(sourceName);
                    }
                }
            } catch (Exception e) {
                logError("Failed to migrate from " + sourceName + ".json: " + e.getMessage());
            }
        }
        return success;
    }
    
    private boolean migrateSettingsFromJson(JsonObject json, String sourceName) {
        boolean foundData = false;
        int keysMigrated = 0;

        try {
            if (json.has(LEGACY_SETTINGS_KEY)) {

                var settingsElement = json.get(LEGACY_SETTINGS_KEY);
                if (settingsElement.isJsonObject()) {
                    int beforeCount = countTotalKeys();
                    foundData = migrateFromMixedSection(json);
                    int afterCount = countTotalKeys();
                    keysMigrated = afterCount - beforeCount;
                    if (foundData) {
                        log("Migrated " + keysMigrated + " settings from " + sourceName);
                    }
                }
            }
            
            if (json.has("modKeys")) {
                JsonElement modKeysElement = json.get("modKeys");
                if (modKeysElement.isJsonObject()) {
                    JsonObject tempJson = new JsonObject();
                    tempJson.add(LEGACY_SETTINGS_KEY, modKeysElement);
                    int beforeCount = countTotalKeys();
                    boolean modKeysMigrated = migrateFromMixedSection(tempJson);
                    int afterCount = countTotalKeys();
                    if (modKeysMigrated) {
                        foundData = true;
                        keysMigrated += (afterCount - beforeCount);
                        log("Merged " + (afterCount - beforeCount) + " modKeys from " + sourceName);
                    }
                }
            }
            
        } catch (Exception e) {
            logError("Error during migration from " + sourceName + ": " + e.getMessage());
        }       
        
        log("Finished migration from " + sourceName + ". Total keys migrated: " + keysMigrated);
        return foundData;
    }
    
    /**
     * Count total keys across all typed sections for validation.
     */
    private int countTotalKeys() {
        return booleans.getValues().size() + 
               integers.getValues().size() + 
               doubles.getValues().size() + 
               strings.getValues().size();
    }
    
    /**
     * Migrate from old mixed-type section to typed sections.
     * Uses ConfigKey.findByString() to determine the type of each value.
     */
    private boolean migrateFromMixedSection(JsonObject json) {
        if (!json.has(LEGACY_SETTINGS_KEY)) return false;
        
        var settingsElement = json.get(LEGACY_SETTINGS_KEY);
        if (!settingsElement.isJsonObject()) return false;
        
        JsonObject settings = settingsElement.getAsJsonObject();
        int migratedCount = 0;
        int failedCount = 0;
        
        for (String keyStr : settings.keySet()) {
            ConfigKey<?> key = ConfigKey.findByString(keyStr);
            
            if (key != null) {

                try {
                    var element = settings.get(keyStr);
                    var value = extractValue(element, key);
                    if (value != null) {
                        setValueUnsafe(key, value);
                        migratedCount++;
                    } else {
                        warn("Failed to extract value for key '" + keyStr + "' - null result");
                        failedCount++;
                    }
                } catch (Exception e) {
                    warn("Failed to migrate key '" + keyStr + "': " + e.getMessage() + " - skipping");
                    failedCount++;
                }

            } else {
                // Unknown/deprecated key
                if (!keyStr.equals("configVersion")) {
                    warn("Unknown config key during migration: " + keyStr);
                }
            }
        }

        if (migratedCount > 0) {
            log("Migrated " + migratedCount + " values from mixed section" + 
                (failedCount > 0 ? " (" + failedCount + " failed)" : ""));
        }
        
        return migratedCount > 0;
    }

    public void removeDeprecatedFiles() {
        if (!isMigrationComplete(OLD_FA_FILE)) return;
        File faFile = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons/fishyaddons.json");
        try {
            if (faFile.exists()) {
                Files.delete(faFile.toPath());
                log("Deleted deprecated file: " + faFile.getName());
            }
        } catch (IOException e) {
            logError("Failed to delete deprecated file " + faFile.getName() + ": " + e.getMessage());
        }
    }       
    
    /**
     * Extract a value from JSON based on the key's type.
     */
    private Object extractValue(JsonElement element, ConfigKey<?> key) {
        if (!element.isJsonPrimitive()) return null;
        
        return switch (key) {
            case BooleanKey bk -> element.getAsBoolean();
            case IntKey ik -> element.getAsInt();
            case DoubleKey dk -> element.getAsDouble();
            case StringKey sk -> element.getAsString();
        };
    }
    
    public static boolean getBoolean(BooleanKey key) {
        return INSTANCE.getValue(key);
    }
    
    public static void setBoolean(BooleanKey key, boolean value) {
        INSTANCE.setValue(key, value);
    }
    
    public static int getInt(IntKey key) {
        return INSTANCE.getValue(key);
    }
    
    public static void setInt(IntKey key, int value) {
        INSTANCE.setValue(key, value);
    }
    
    public static double getDouble(DoubleKey key) {
        return INSTANCE.getValue(key);
    }
    
    public static void setDouble(DoubleKey key, double value) {
        INSTANCE.setValue(key, value);
    }
    
    public static String getString(StringKey key) {
        return INSTANCE.getValue(key);
    }
    
    public static void setString(StringKey key, String value) {
        INSTANCE.setValue(key, value);
    }
}
