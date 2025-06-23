package me.wait.fishyaddons.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;
import me.wait.fishyaddons.handlers.KeybindHandler;
import me.wait.fishyaddons.handlers.FishyLavaHandler;
import me.wait.fishyaddons.event.ClientConnectedToServer;
import me.wait.fishyaddons.handlers.AliasHandler;
import me.wait.fishyaddons.util.SkyblockCheck;
import com.google.gson.stream.MalformedJsonException;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class ConfigHandler {
    private static final File CONFIG_FILE = new File("config/fishyaddons/fishyaddons.json");
    private static final File BACKUP_DIR = new File("config/fishyaddons/backup");
    private static final File BACKUP_FILE = new File(BACKUP_DIR, "fishyaddons.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, String> keybinds = new HashMap<>();
    private static final Map<String, Boolean> toggledKeybinds = new HashMap<>();
    private static final Map<String, String> commandAliases = new HashMap<>();
    private static final Map<String, Boolean> toggledCommands = new HashMap<>();

    private static float red = 0.4f, green = 1.0f, blue = 1.0f;
    private static int customParticleColorIndex = 0;

    private static boolean firstLoad = false;
    private static boolean recreatedConfig = false;
    private static boolean restoredConfig = false;
    private static boolean isFishyLavaEnabled = true;
    private static boolean customParticlesEnabled = true;
    private static boolean configChanged = false;

    public static boolean isFirstLoad() { return firstLoad; }
    public static boolean isRecreated() { return recreatedConfig; }
    public static boolean isRestored() { return restoredConfig; }
    public static void resetFlags() {
        recreatedConfig = false;
        restoredConfig = false;
        firstLoad = false;
    }

    public static void init() {
        CONFIG_FILE.getParentFile().mkdirs(); // Ensure the config directory exists
        BACKUP_DIR.mkdirs(); // Ensure the backup directory exists
        load();
    }

    public static void markConfigChanged() {
        configChanged = true;
    }

    public static void saveConfigIfNeeded() {
        if (configChanged) {
            save();
            configChanged = false;
        }
    }

    public static synchronized void load() {
        if (!CONFIG_FILE.exists()) {
            System.err.println("[ConfigHandler] Config file does not exist. Creating a new one...");
            loadOrRestore();
            firstLoad = true;
            return;
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            // Validate and load the configuration
            if (json == null || !validate(json)) {
                System.err.println("[ConfigHandler] Invalid config detected. Attempting to restore from backup...");
                loadOrRestore();
                return;
            }

            // Load configuration values
            if (json.has("keybinds")) {
                Type keybindsType = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> loadedKeybinds = GSON.fromJson(json.get("keybinds"), keybindsType);
                keybinds.clear();
                keybinds.putAll(loadedKeybinds);
            }

            if (json.has("toggledKeybinds")) {
                Type toggledType = new TypeToken<Map<String, Boolean>>() {}.getType();
                Map<String, Boolean> loadedToggled = GSON.fromJson(json.get("toggledKeybinds"), toggledType);
                toggledKeybinds.clear();
                toggledKeybinds.putAll(loadedToggled);
            }

            if (json.has("commandAliases")) {
                Type aliasesType = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> loadedAliases = GSON.fromJson(json.get("commandAliases"), aliasesType);
                commandAliases.clear();
                commandAliases.putAll(loadedAliases);
            }

            if (json.has("toggledCommands")) {
                Type toggledType = new TypeToken<Map<String, Boolean>>() {}.getType();
                Map<String, Boolean> loadedToggled = GSON.fromJson(json.get("toggledCommands"), toggledType);
                toggledCommands.clear();
                toggledCommands.putAll(loadedToggled);
            }

            if (json.has("customParticlesEnabled")) {
                customParticlesEnabled = json.get("customParticlesEnabled").getAsBoolean();
            }

            if (json.has("customParticleColorIndex")) {
                int loadedIndex = json.get("customParticleColorIndex").getAsInt();
                customParticleColorIndex = Math.max(0, Math.min(4, loadedIndex));
            }

            if (json.has("red")) red = json.get("red").getAsFloat();
            if (json.has("green")) green = json.get("green").getAsFloat();
            if (json.has("blue")) blue = json.get("blue").getAsFloat();
            if (json.has("isFishyLavaEnabled")) {
                isFishyLavaEnabled = json.get("isFishyLavaEnabled").getAsBoolean();
            }
        } catch (JsonSyntaxException | MalformedJsonException e) {
            System.err.println("[TextureConfig] Malformed JSON detected: " + e.getMessage());
            loadOrRestore();
        } catch (IOException e) {
            System.err.println("[ConfigHandler] Failed to load configuration: " + e.getMessage());
            loadOrRestore();
        }
    }

    public static synchronized void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            JsonObject json = new JsonObject();

            json.add("keybinds", GSON.toJsonTree(keybinds));
            json.add("toggledKeybinds", GSON.toJsonTree(toggledKeybinds));
            json.add("commandAliases", GSON.toJsonTree(commandAliases));
            json.add("toggledCommands", GSON.toJsonTree(toggledCommands));

            json.addProperty("customParticlesEnabled", customParticlesEnabled);
            json.addProperty("customParticleColorIndex", customParticleColorIndex);
            json.addProperty("red", red);
            json.addProperty("green", green);
            json.addProperty("blue", blue);
            json.addProperty("isFishyLavaEnabled", isFishyLavaEnabled);

            GSON.toJson(json, writer);
        } catch (IOException e) {
            System.err.println("[ConfigHandler] Failed to save configuration: " + e.getMessage());
        }
    }

    public static void saveBackup() {
        try {
            if (CONFIG_FILE.exists()) {
                Files.copy(CONFIG_FILE.toPath(), BACKUP_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[ConfigHandler] Failed to save backup: " + e.getMessage());
        }
    }

    private static boolean validate(JsonObject json) {
        // Example validation: Check for required fields
        return json.has("keybinds") && json.has("customParticlesEnabled") && json.has("customParticleColorIndex") 
                && json.has("red") && json.has("green") && json.has("blue")
                && json.has("isFishyLavaEnabled") && json.has("toggledKeybinds")
                && json.has("commandAliases") && json.has("toggledCommands");
    }

    private static void loadOrRestore() {
        if (BACKUP_FILE.exists()) {
            System.err.println("[ConfigHandler] Restoring from backup...");
            try {
                Files.copy(BACKUP_FILE.toPath(), CONFIG_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                load();
                restoredConfig = true;
                return;
            } catch (IOException e) {
                System.err.println("[ConfigHandler] Failed to restore from backup: " + e.getMessage());
            }
        }

        System.err.println("[ConfigHandler] No valid backup found. Creating a new default configuration...");
        save();
        recreatedConfig = true;
    }

    public static boolean isCustomParticlesEnabled() {
        return customParticlesEnabled;
    }
    
    public static void setCustomParticlesEnabled(boolean enabled) {
        customParticlesEnabled = enabled;
        markConfigChanged();
    }

    public static float[] getParticleColor() {
        switch (customParticleColorIndex) {
            case 0: return new float[]{0.4f, 1.0f, 1.0f};
            case 1: return new float[]{0.4f, 1.0f, 0.6f};
            case 2: return new float[]{1.0f, 0.8f, 1.0f};
            case 3: return new float[]{0.9f, 0.9f, 1.0f};
            default: return null; // Default red
        }
    }
    
    public static void setColor(float r, float g, float b) {
        red = r;
        green = g;
        blue = b;
        markConfigChanged();
    }

    public static synchronized void setCustomParticleColorIndex(int index) {
        if (index < 0 || index > 4) {
            customParticleColorIndex = 0;
        } else {
            customParticleColorIndex = index;
        }

        ParticleColorConfig.invalidateCache();
        markConfigChanged();          
    }

    public static synchronized int getCustomParticleColorIndex() {
        if (customParticleColorIndex < 0 || customParticleColorIndex > 4) {
            customParticleColorIndex = 0;
        }
        return customParticleColorIndex;
    }
    
    public static Map<String, String> getKeybinds() {
        return keybinds;
    }

    public static void setKeybindCommand(String key, String command) {
        keybinds.put(key, command);
        markConfigChanged();
        KeybindHandler.refreshKeybindCache();
    }

    public static void removeKeybind(String key) {
        keybinds.remove(key);
        markConfigChanged();
        KeybindHandler.refreshKeybindCache();
    }

    public static boolean isKeybindToggled(String key) {
        return toggledKeybinds.getOrDefault(key, true);
    }

    public static void toggleKeybind(String key, boolean enabled) {
        toggledKeybinds.put(key, enabled);
        markConfigChanged();
    }

    public static String getKeybindCommand(String key) {
        return keybinds.get(key);
    }

    public static boolean isFishyLavaEnabled() {
        return isFishyLavaEnabled;
    }

    public static void setFishyLavaEnabled(boolean enabled) {
        isFishyLavaEnabled = enabled;
        markConfigChanged();
        ClientConnectedToServer.refreshServerData();
        FishyLavaHandler.updateRegistration();
    }

    public static Map<String, String> getCommandAliases() {
        return commandAliases;
    }

    public static void setCommandAlias(String alias, String command) {
        if (!alias.startsWith("/")) alias = "/" + alias; // normalize
        commandAliases.put(alias, command);
        toggledCommands.putIfAbsent(alias, true);
        save();
        AliasHandler.refreshCommandCache();
    }
    

    public static void removeCommandAlias(String alias) {
        commandAliases.remove(alias);
        save();
        AliasHandler.refreshCommandCache();
    }

    public static boolean isCommandToggled(String alias) {
        if (!alias.startsWith("/")) alias = "/" + alias;
        return toggledCommands.getOrDefault(alias, true);
    }
    

    public static void toggleCommand(String alias, boolean enabled) {
        if (!alias.startsWith("/")) alias = "/" + alias;
        toggledCommands.put(alias, enabled);
        save();
        AliasHandler.refreshCommandCache();
    }

    public static String getCommandAlias(String alias) {
        return commandAliases.get(alias);
    }
}