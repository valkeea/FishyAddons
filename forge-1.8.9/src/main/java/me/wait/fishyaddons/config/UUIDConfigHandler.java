package me.wait.fishyaddons.config;

import me.wait.fishyaddons.handlers.SellProtectionHandler;
import me.wait.fishyaddons.fishyprotection.BlacklistConfigHandler;
import me.wait.fishyaddons.fishyprotection.BlacklistMatcher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class UUIDConfigHandler {
    private static final File CONFIG_FILE = new File("config/fishyaddons/fishyitems.json");
    private static final File BACKUP_DIR = new File("config/fishyaddons/backup");
    private static final File BACKUP_FILE = new File(BACKUP_DIR, "fishyitems.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, String> protectedUUIDs = new HashMap<>();

    private static boolean configChanged = false;
    private static boolean initialized = false;
    private static boolean isSellProtectionEnabled = true;
    private static boolean isTooltipEnabled = true;
    private static boolean isProtectTriggerEnabled = true;
    private static boolean isProtectNotiEnabled = true;
    private static boolean recreatedConfig = false;
    private static boolean restoredConfig = false;

    public static boolean isRecreated() { return recreatedConfig; }
    public static boolean isRestored() { return restoredConfig; }
    public static void resetFlags() {
        recreatedConfig = false;
        restoredConfig = false;
    }

    public static synchronized void init() {
        CONFIG_FILE.getParentFile().mkdirs();
        BACKUP_DIR.mkdirs();
        if (!initialized) {
            load();
            initialized = true;
        }
    }

    public static synchronized void addUUID(String uuid, String displayName) {
        if (!protectedUUIDs.containsKey(uuid) || !Objects.equals(protectedUUIDs.get(uuid), displayName)) {
            protectedUUIDs.put(uuid, displayName);
            configChanged = true;
            saveConfigIfNeeded();
        }
    }

    public static synchronized void removeUUID(String uuid) {
        if (protectedUUIDs.remove(uuid) != null) {
            configChanged = true;
            saveConfigIfNeeded();
        }
    }

    public static synchronized void clearAll() {
        protectedUUIDs.clear();
        configChanged = true;
        saveConfigIfNeeded();
    }

    public static synchronized boolean isProtected(String uuid) {
        return protectedUUIDs.containsKey(uuid);
    }

    public static synchronized String getDisplayName(String uuid) {
        return protectedUUIDs.get(uuid);
    }

    public static synchronized Map<String, String> getProtectedUUIDs() {
        return new HashMap<>(protectedUUIDs);
    }

    public static boolean isSellProtectionEnabled() {
        return isSellProtectionEnabled;
    }

    public static void setSellProtectionEnabled(boolean enabled) {
        isSellProtectionEnabled = enabled;
        if (enabled) {
            SellProtectionHandler.register();
        } else {
            SellProtectionHandler.unregister();
        }
        save();
    }

    public static boolean isProtectTriggerEnabled() {
        return isProtectTriggerEnabled;
    }

    public static void setProtectTriggerEnabled(boolean enabled) {
        isProtectTriggerEnabled = enabled;
        markConfigChanged();
    }

    public static boolean isProtectNotiEnabled() {
        return isProtectNotiEnabled;
    }

    public static void setProtectNotiEnabled(boolean enabled) {
        isProtectNotiEnabled = enabled;
        markConfigChanged();
    }

    public static boolean isTooltipEnabled() {
        return isTooltipEnabled;
    }

    public static void setTooltipEnabled(boolean enabled) {
        isTooltipEnabled = enabled;
        markConfigChanged();
    }

    public static synchronized void load() {
        if (!CONFIG_FILE.exists()) {
            System.err.println("[UUIDConfigHandler] Config file does not exist. Creating a new one...");
            loadOrRestore();
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(CONFIG_FILE), "UTF-8")) { // <-- Use UTF-8
            Type configType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> config = GSON.fromJson(reader, configType);

            if (config == null || !validate(config)) {
                System.err.println("[UUIDConfigHandler] Invalid config detected. Attempting to restore from backup...");
                loadOrRestore();
                return;
            }

            if (config.containsKey("sellProtectionEnabled")) {
                isSellProtectionEnabled = (Boolean) config.get("sellProtectionEnabled");
            }

            if (config.containsKey("tooltipEnabled")) {
                isTooltipEnabled = (Boolean) config.get("tooltipEnabled");
            }

            if (config.containsKey("protectTriggerEnabled")) {
                isProtectTriggerEnabled = (Boolean) config.get("protectTriggerEnabled");
            }

            if (config.containsKey("protectNotiEnabled")) {
                isProtectNotiEnabled = (Boolean) config.get("protectNotiEnabled");
            }

            if (config.containsKey("protectedUUIDs")) {
                Object uuidsObj = config.get("protectedUUIDs");
                protectedUUIDs.clear();
                if (uuidsObj instanceof Map) {
                    Map<?, ?> uuidMap = (Map<?, ?>) uuidsObj;
                    for (Map.Entry<?, ?> entry : uuidMap.entrySet()) {
                        if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                            protectedUUIDs.put((String) entry.getKey(), (String) entry.getValue());
                        }
                    }
                } else if (uuidsObj instanceof List) {
                    // Backward compatibility: old format was a list of UUIDs
                    for (Object uuidObj : (List<?>) uuidsObj) {
                        if (uuidObj instanceof String) {
                            protectedUUIDs.put((String) uuidObj, "");
                        }
                    }
                }
            }

            if (config.containsKey("blacklist")) {
                Object blacklistObject = config.get("blacklist");
                if (blacklistObject instanceof List) {
                    List<?> rawList = (List<?>) blacklistObject;
                    List<Map<String, Object>> entries = new ArrayList<>();
                    for (Object o : rawList) {
                        if (o instanceof Map) {
                            entries.add((Map<String, Object>) o);
                        }
                    }
                    BlacklistConfigHandler.loadUserBlacklistFromJson(entries);
                } else {
                    System.err.println("[UUIDConfigHandler] Blacklist is not a valid list.");
                }
            }
        } catch (JsonSyntaxException | MalformedJsonException e) {
            System.err.println("[UUIDConfigHandler] Malformed JSON detected: " + e.getMessage());
            loadOrRestore();
        } catch (IOException e) {
            System.err.println("[UUIDConfigHandler] Failed to load configuration: " + e.getMessage());
            loadOrRestore();
        }
    }

    public static synchronized void save() {
        Map<String, Object> config = new HashMap<>();
        config.put("protectedUUIDs", new HashMap<>(protectedUUIDs));
        config.put("sellProtectionEnabled", isSellProtectionEnabled);
        config.put("tooltipEnabled", isTooltipEnabled);
        config.put("protectTriggerEnabled", isProtectTriggerEnabled);
        config.put("protectNotiEnabled", isProtectNotiEnabled);

        List<Map<String, Object>> serializedBlacklist = BlacklistConfigHandler.getUserBlacklistAsJson();
        config.put("blacklist", serializedBlacklist);
        System.out.println("[UUIDConfigHandler] Saving blacklist to config...");

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE), "UTF-8")) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("[UUIDConfigHandler] Failed to save config: " + e.getMessage());
        }
    }

    public static void saveBackup() {
        try {
            if (CONFIG_FILE.exists()) {
                Files.copy(CONFIG_FILE.toPath(), BACKUP_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[UUIDConfigHandler] Backup saved successfully.");
            }
        } catch (IOException e) {
            System.err.println("[UUIDConfigHandler] Failed to save backup: " + e.getMessage());
        }
    }

    private static void loadOrRestore() {
        if (BACKUP_FILE.exists()) {
            System.err.println("[UUIDConfigHandler] Restoring from backup...");
            try {
                Files.copy(BACKUP_FILE.toPath(), CONFIG_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                load();
                restoredConfig = true;
                return;
            } catch (IOException e) {
                System.err.println("[UUIDConfigHandler] Failed to restore from backup: " + e.getMessage());
            }
        }

        System.err.println("[UUIDConfigHandler] No valid backup found. Creating a new default configuration...");
        save();
        recreatedConfig = true;
    }

    private static boolean validate(Map<String, Object> config) {
        return config.containsKey("sellProtectionEnabled") && config.containsKey("tooltipEnabled") &&
               config.containsKey("protectTriggerEnabled") && config.containsKey("protectNotiEnabled") &&
               config.containsKey("protectedUUIDs");
    }

    public static void markConfigChanged() {
        configChanged = true;
    }

    public static synchronized void saveConfigIfNeeded() {
        if (configChanged) {
            save();
            configChanged = false;
        }
    }
}