package me.valkeea.fishyaddons.vconfig.config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import me.valkeea.fishyaddons.vconfig.api.ConfigKey;
import net.minecraft.client.MinecraftClient;

public abstract class BaseConfig {
    protected static final String VERSION_KEY = "configVersion";
    protected static final int CURRENT_VERSION = 1;
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    protected final File configFile;
    protected final File backupDir;
    
    private int batchDepth = 0;
    private boolean pendingSave = false;
    
    private boolean recreatedConfig = false;
    private boolean restoredConfig = false;
    
    protected BaseConfig(String filename) {
        File root = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons");
        root.mkdirs();
        this.configFile = new File(root, filename);
        this.backupDir = new File(root, "backup");
        backupDir.mkdirs();
    }
    
    protected File getMigrationMarker(String sourceName) {
        File root = configFile.getParentFile();
        return new File(root, ".migrated_" + CURRENT_VERSION + "_" + sourceName.replace(".json", ""));
    }
    
    protected boolean isMigrationComplete(String sourceName) {
        return getMigrationMarker(sourceName).exists();
    }
    
    protected void markMigrationComplete(String sourceName) {
        try {
            File marker = getMigrationMarker(sourceName);
            if (!marker.exists() && !marker.createNewFile()) {
                logError("Failed to create migration marker file for " + sourceName);
            } else {
                log("Marked migration complete: " + sourceName);
            }
        } catch (IOException e) {
            logError("Failed to create migration marker for " + sourceName);
        }
    }
    
    protected void backupOldConfigFile(File oldFile) {
        if (!oldFile.exists()) return;
        
        try {
            String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            File datedBackupDir = new File(backupDir, "migration_" + timestamp);
            datedBackupDir.mkdirs();
            File backupFile = new File(datedBackupDir, oldFile.getName());
            Files.copy(oldFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log("Backed up " + oldFile.getName() + " before migration");
        } catch (IOException e) {
            logError("Failed to backup old config file: " + oldFile.getName());
        }
    }
    
    /**
     * Load sections from JSON.
     * Called after JSON is validated and parsed.
     */
    protected abstract void loadFromJson(JsonObject json);
    
    /** Save sections to JSON. */
    protected abstract void saveToJson(JsonObject json);
    
    /** Validate before loading. */
    protected boolean validate(JsonObject json) {
        return json != null;
    }
    
    /** Get the config file name for logging. */
    public String getConfigName() {
        return configFile.getName();
    }
    
    /** Check if the config was recreated */
    public boolean isRecreated() {
        return recreatedConfig;
    }
    
    /** Check if the config was restored from backup. */
    public boolean isRestored() {
        return restoredConfig;
    }
    
    /** Reset status flags. */
    public void resetFlags() {
        recreatedConfig = false;
        restoredConfig = false;
    }
    
    /** Create parent directories and load. */
    protected void init() {
        configFile.getParentFile().mkdirs();
        load();
    }
    
    /**
     * Load configuration from disk.
     * Handles migration if needed.
     */
    protected synchronized void load() {
        if (!configFile.exists()) {
            if (!tryMigrate()) {
                recreatedConfig = true;
                save();
            }
            return;
        }
        
        try (var reader = new FileReader(configFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            
            if (!validate(json)) {
                warn("Config validation failed, attempting restore");
                saveBackup();
                tryRestore();
                return;
            }
            
            incrementDepth();
            try {
                loadFromJson(json);
            } finally {
                pendingSave = false; // Clear pending save (data came from disk)
                decrementDepthAndFlush();
            }
            
        } catch (JsonSyntaxException | JsonIOException e) {
            logError("Malformed JSON: " + e.getMessage());
            saveBackup(); // Save corrupted file as backup for debugging
            tryRestore();
        } catch (IOException e) {
            logError("Failed to read config: " + e.getMessage());
            tryRestore();
        } catch (Exception e) {
            logError("Unexpected error loading config: " + e.getMessage());
            saveBackup();
            tryRestore();
        }
    }

    private synchronized void save() {
        if (batchDepth > 0) {
            pendingSave = true;
            return;
        }
        
        File tempFile = new File(configFile.getParentFile(), configFile.getName() + ".tmp");
        try {
            try (var writer = new FileWriter(tempFile)) {
                JsonObject json = new JsonObject();
                saveToJson(json);
                GSON.toJson(json, writer);
            }
            
            Files.move(tempFile.toPath(), configFile.toPath(), 
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                
        } catch (IOException e) {
            logError("Failed to save config: ", e);
            if (tempFile.exists()) { // Clean up temp file on failure
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException ex) {
                    // Ignore cleanup failure
                }
            }
        }
    }
    
    /**
     * Request a save (respects batch mode).
     */
    protected void requestSave() {
        save();
    }
    
    /**
     * Create a backup of the current config file.
     */
    public void saveBackup() {
        try {
            if (configFile.exists()) {
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                File datedBackupDir = new File(backupDir, "bu_fa_" + timestamp);
                datedBackupDir.mkdirs();
                File backupFile = new File(datedBackupDir, configFile.getName());
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                File[] backups = backupDir.listFiles(File::isDirectory);
                if (backups != null && backups.length > 5) {
                    java.util.Arrays.sort(backups, (a, b) -> a.getName().compareTo(b.getName())); // oldest first
                    for (int i = 0; i < backups.length - 5; i++) {
                        deleteDirectory(backups[i]);
                    }
                }
            }
        } catch (IOException e) {
            logError("Failed to create backup");
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectory(f);
                }
            }
        }
        try {
            Files.delete(dir.toPath());
        } catch (IOException e) {
            logError("Failed to delete directory: " + dir.getAbsolutePath());
        }
    }
    
    /**
     * By default attempts to migrate from the old monolithic file
     */
    protected boolean tryMigrate() {
        File oldConfig = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons/fishyaddons.json");
        if (!oldConfig.exists()) return false;
        
        try (var reader = new FileReader(oldConfig)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) return false;
            
            loadFromJson(json);
            save();
            return true;
        } catch (Exception e) {
            logError("Migration failed", e);
            return false;
        }
    }
    
    /**
     * Restore from backup if it exists, otherwise create new config.
     */
    protected void tryRestore() {
        File[] backups = backupDir.listFiles(File::isDirectory);

        if (backups != null && backups.length > 0) {
            java.util.Arrays.sort(backups, (a, b) -> b.getName().compareTo(a.getName()));
            boolean restored = false;

            for (File datedBackup : backups) {

                File backupFile = new File(datedBackup, configFile.getName());
                if (backupFile.exists()) {
                    try {
                        Files.copy(backupFile.toPath(), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        restoredConfig = true;
                        load();
                        warn("Restored from backup: " + datedBackup.getName());
                        restored = true;
                        break;
                    } catch (IOException e) {
                        logError("Backup restore failed for " + datedBackup.getName());
                    }
                }
            }
            if (!restored) {
                recreatedConfig = true;
                save();
            }

        } else {
            recreatedConfig = true;
            save();
        }
    }
    
    /** Begin a batch of config changes for this config instance. */
    public void incrementDepth() {
        batchDepth++;
    }
    
    /**
     * End a batch of config changes for this config instance.
     * Flushes pending save when batch completes.
     */
    public void decrementDepthAndFlush() {
        batchDepth = Math.max(0, batchDepth - 1);
        if (batchDepth == 0) {
            flushPendingSave();
        }
    }
    
    /**
     * Flush any pending save in this config instance.
     */
    protected void flushPendingSave() {
        if (pendingSave) {
            pendingSave = false;
            try (Writer writer = new FileWriter(configFile)) {
                JsonObject json = new JsonObject();
                saveToJson(json);
                GSON.toJson(json, writer);
            } catch (IOException e) {
                logError("Failed to save config", e);
            }
        }
    }
    
    /**
     * Get the appropriate typed section for a config key.
     * Subclasses override this to implement routing logic for typed sections.
     * Default implementation throws an exception.
     * 
     * @param key The typed configuration key
     * @param <T> The type of value the key stores
     * @return The appropriate ConfigSection for this key
     */
    protected <T> ConfigSection<T> getSectionForKey(ConfigKey<T> key) {
        throw new UnsupportedOperationException(
            "Config " + getConfigName() + " does not support typed key access. Use the local API instead.");
    }
    
    /**
     * Get a value by typed key.
     * Routing is handled by subclasses via {@link #getSectionForKey(ConfigKey)}.
     * 
     * @param key The typed configuration key
     * @param <T> The type of value
     * @return The value, or the key's default if not set
     */
    protected <T> T getValue(ConfigKey<T> key) {
        ConfigSection<T> section = getSectionForKey(key);
        return section.get(key);
    }
    
    /**
     * Set a value by typed key.
     * Routing is handled by subclasses via {@link #getSectionForKey(ConfigKey)}.
     * Triggers save and notifies listeners.
     * 
     * @param key The typed configuration key
     * @param value The value to set
     * @param <T> The type of value
     */
    protected <T> void setValue(ConfigKey<T> key, T value) {
        ConfigSection<T> section = getSectionForKey(key);
        section.set(key, value);
    }
    
    /**
     * Unsafe set
     * Used internally during migration from old config formats.
     * 
     * @param key The typed configuration key
     * @param value The value to set (cast to appropriate type)
     * @param <T> The type of value
     */
    @SuppressWarnings("unchecked")
    protected <T> void setValueUnsafe(ConfigKey<T> key, Object value) {
        ConfigSection<T> section = getSectionForKey(key);
        section.set(key.getString(), (T) value);
    }

    protected static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BaseConfig.class);
    private static final String INSERT = "[{}] {}";

    protected void logError(String message) {
        LOGGER.error(INSERT, getConfigName(), message);
    }

    protected void logError(String message, Throwable t) {
        LOGGER.error(INSERT, getConfigName(), message, t);
    }

    protected void warn(String message) {
        LOGGER.warn(INSERT, getConfigName(), message);
    }

    protected void log(String message) {
        LOGGER.info(INSERT, getConfigName(), message);
    }

    protected boolean needsMigration(JsonObject json) {
        return !json.has(VERSION_KEY) || json.get(VERSION_KEY).getAsInt() < CURRENT_VERSION;
    }    
}
