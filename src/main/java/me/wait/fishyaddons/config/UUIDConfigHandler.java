package me.wait.fishyaddons.config;

import me.wait.fishyaddons.handlers.SellProtectionHandler;
import me.wait.fishyaddons.fishyprotection.BlacklistConfigHandler;
import me.wait.fishyaddons.fishyprotection.BlacklistMatcher;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class UUIDConfigHandler {
    private static final File configFile = new File("config", "fishyitems.json");
    private static final Gson GSON = new Gson();
    private static final Set<String> protectedUUIDs = new HashSet<>();
    private static boolean configChanged = false;
    private static boolean initialized = false;
    private static boolean isSellProtectionEnabled = true;
    private static boolean isTooltipEnabled = true;
    private static boolean isProtectTriggerEnabled = true;
    private static boolean isProtectNotiEnabled = true;

    public static synchronized void init() {
        if (!initialized) {
            loadConfig();
            initialized = true;
        }
    }

    public static synchronized void addUUID(String uuid) {
        if (protectedUUIDs.add(uuid)) {
            configChanged = true;
            saveConfigIfNeeded();
        }
    }

    public static synchronized void removeUUID(String uuid) {
        if (protectedUUIDs.remove(uuid)) {
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
        return protectedUUIDs.contains(uuid);
    }

    public static synchronized Set<String> getProtectedUUIDs() {
        return new HashSet<>(protectedUUIDs);
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
        saveConfig();
    }
    
    
    public static boolean isProtectTriggerEnabled() {
        return isProtectTriggerEnabled;
    }

    public static void setProtectTriggerEnabled(boolean enabled) {
        isProtectTriggerEnabled = enabled;
        configChanged = true;
        saveConfigIfNeeded();
    }

    public static boolean isProtectNotiEnabled() {
        return isProtectNotiEnabled;
    }

    public static void setProtectNotiEnabled(boolean enabled) {
        isProtectNotiEnabled = enabled;
        configChanged = true;
        saveConfigIfNeeded();
    }

    public static boolean isTooltipEnabled() {
        return isTooltipEnabled;
    }

    public static void setTooltipEnabled(boolean enabled) {
        isTooltipEnabled = enabled;
        configChanged = true;
        saveConfigIfNeeded();
    }

    public static synchronized void loadConfig() {
        if (!configFile.exists()) {
            System.out.println("[UUIDConfigHandler] Config file does not exist. Creating a new one...");
            saveConfig();
            return;
        }
    
        Map<String, Object> config = null;
    
        try (Reader reader = new FileReader(configFile)) {
            Type configType = new TypeToken<Map<String, Object>>() {}.getType();
            config = GSON.fromJson(reader, configType);
    
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
                if (uuidsObj instanceof List) {
                    protectedUUIDs.clear();
                    for (Object uuidObj : (List<?>) uuidsObj) {
                        if (uuidObj instanceof String) {
                            protectedUUIDs.add((String) uuidObj);
                        }
                    }
                }
            }
    
            if (config.containsKey("blacklist")) {
                // Load the GUI blacklist and forward it to BlacklistConfigHandler
                Object blacklistObject = config.get("blacklist");
                if (blacklistObject instanceof List) {
                    List<?> rawList = (List<?>) blacklistObject;
                    List<Map<String, Object>> entries = new ArrayList<>();
                    for (Object o : rawList) {
                        if (o instanceof Map) {
                            entries.add((Map<String, Object>) o);
                        }
                    }
                    BlacklistConfigHandler.getUserBlacklistAsJson(); // Corrected call
                } else {
                    System.err.println("[UUIDConfigHandler] Blacklist is not a valid list.");
                }
            }
        } catch (IOException e) {
            System.err.println("[UUIDConfigHandler] Failed to load config: " + e.getMessage());
        }
    }

    private static synchronized void saveConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("protectedUUIDs", new ArrayList<>(protectedUUIDs));
        config.put("sellProtectionEnabled", isSellProtectionEnabled);
        config.put("tooltipEnabled", isTooltipEnabled);
        config.put("protectTriggerEnabled", isProtectTriggerEnabled);
        config.put("protectNotiEnabled", isProtectNotiEnabled);

        // Serialize the blacklist properly
        List<Map<String, Object>> serializedBlacklist = BlacklistConfigHandler.getUserBlacklistAsJson();
        config.put("blacklist", serializedBlacklist);

        try (Writer writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("[UUIDConfigHandler] Failed to save config: " + e.getMessage());
        }
    }

    public static void markConfigChanged() {
        configChanged = true;
    }
    

    public static synchronized void saveConfigIfNeeded() {
        if (configChanged) {
            saveConfig();
            configChanged = false;
        }
    }
}
