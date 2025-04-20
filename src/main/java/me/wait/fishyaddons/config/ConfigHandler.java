package me.wait.fishyaddons.config;

import me.wait.fishyaddons.handlers.KeybindHandler;
import me.wait.fishyaddons.handlers.FishyLavaHandler;
import me.wait.fishyaddons.config.ParticleColorConfig;
import me.wait.fishyaddons.handlers.AliasHandler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;


public class ConfigHandler {
    private static File configFile;
    private static final Gson GSON = new Gson();

    private static final Map<String, String> keybinds = new HashMap<>();
    private static final Map<String, Boolean> toggledKeybinds = new HashMap<>();
    private static final Map<String, String> commandAliases = new HashMap<>();
    private static final Map<String, Boolean> toggledCommands = new HashMap<>();

    private static float red = 0.4f, green = 1.0f, blue = 1.0f;  
    private static int customParticleColorIndex = 0;

    private static boolean isFishyLavaEnabled = true;
    private static boolean customParticlesEnabled = true;
    private static boolean debugEnabled = true;
    private static boolean configChanged = false;
    private static boolean configLoaded = false;

    static {
        if (configFile == null) {
            configFile = new File("config", "fishyaddons.cfg");
        }
        loadConfig();
    }

    public static void initConfigPath(File modConfigDir) {
        File newFile = new File(modConfigDir, "fishyaddons.cfg");
        if (!newFile.equals(configFile)) {
            configFile = newFile;
            loadConfig(); // reload with updated path
        }
    }
    
    private static void markConfigChanged() {
        configChanged = true;
    }

    public static void saveConfigIfNeeded() {
        if (configChanged) {
            saveConfig();
            configChanged = false;
        }
    }

    public static synchronized void loadConfig() {   
        if (configFile == null) {
            throw new IllegalStateException("Config file path not initialized. Call initConfigPath() first.");
        }
        if (!configFile.exists()) {
            saveConfig();
            return;
        }

        try (Reader reader = new FileReader(configFile)) {
            com.google.gson.stream.JsonReader jsonReader = new com.google.gson.stream.JsonReader(reader);
            jsonReader.setLenient(true); // Enable lenient mode to handle malformed JSON

            JsonObject json = GSON.fromJson(jsonReader, JsonObject.class);

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
                if (loadedIndex < 0 || loadedIndex > 4) {
                    customParticleColorIndex = 0; // Reset to default
                } else {
                    customParticleColorIndex = loadedIndex;
                }
            }

            if (json.has("red")) red = json.get("red").getAsFloat();
            if (json.has("green")) green = json.get("green").getAsFloat();
            if (json.has("blue")) blue = json.get("blue").getAsFloat();
            if (json.has("isFishyLavaEnabled")) {
                isFishyLavaEnabled = json.get("isFishyLavaEnabled").getAsBoolean();
            }
        } catch (IOException e) {
            System.err.println("[ConfigHandler] Failed to load configuration: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ConfigHandler] Malformed JSON in configuration file. Recreating file with default values...");
            saveConfig();
        }
    }

    public static synchronized void saveConfig() {
        if (configFile == null) {
            throw new IllegalStateException("Config file path not initialized. Call initConfigPath() first.");
        }

        try (Writer writer = new FileWriter(configFile)) {
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
        FishyLavaHandler.updateRegistration();
    }

    public static Map<String, String> getCommandAliases() {
        return commandAliases;
    }

    public static void setCommandAlias(String alias, String command) {
        if (!alias.startsWith("/")) alias = "/" + alias; // normalize
        commandAliases.put(alias, command);
        toggledCommands.putIfAbsent(alias, true);
        saveConfig();
        AliasHandler.refreshCommandCache();
    }
    

    public static void removeCommandAlias(String alias) {
        commandAliases.remove(alias);
        saveConfig();
        AliasHandler.refreshCommandCache();
    }

    public static boolean isCommandToggled(String alias) {
        if (!alias.startsWith("/")) alias = "/" + alias;
        return toggledCommands.getOrDefault(alias, true);
    }
    

    public static void toggleCommand(String alias, boolean enabled) {
        if (!alias.startsWith("/")) alias = "/" + alias;
        toggledCommands.put(alias, enabled);
        saveConfig();
        AliasHandler.refreshCommandCache();
    }

    public static String getCommandAlias(String alias) {
        return commandAliases.get(alias);
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        markConfigChanged();
    }

}