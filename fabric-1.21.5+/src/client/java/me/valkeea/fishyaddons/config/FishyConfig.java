package me.valkeea.fishyaddons.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.handler.CommandAlias;
import me.valkeea.fishyaddons.handler.KeyShortcut;
import me.valkeea.fishyaddons.handler.ChatReplacement;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FishyConfig {
    private static final File CONFIG_FILE;
    private static final File BACKUP_DIR;
    private static final File BACKUP_FILE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Config section handler
    public static class ConfigSection<V> {
        private final Map<String, V> values;
        private final Map<String, Boolean> toggled;
        private final String valuesKey;
        private final String toggledKey;
        private final Type valueType;
        private final Type toggledType;
        private final Consumer<Void> onChange;

        public ConfigSection(String valuesKey, String toggledKey, Type valueType, Type toggledType, Consumer<Void> onChange) {
            this.values = new LinkedHashMap<>();
            this.toggled = new LinkedHashMap<>();
            this.valuesKey = valuesKey;
            this.toggledKey = toggledKey;
            this.valueType = valueType;
            this.toggledType = toggledType;
            this.onChange = onChange;
        }

        public Map<String, V> getValues() { return values; }
        public Map<String, Boolean> getToggled() { return toggled; }

        public void set(String key, V value) {
            if (key == null || key.isEmpty()) return;
            values.put(key, value);
            toggled.putIfAbsent(key, true);
            if (onChange != null) onChange.accept(null);
        }

        public void remove(String key) {
            if (key == null || key.isEmpty()) return;
            values.remove(key);
            toggled.remove(key);
            if (onChange != null) onChange.accept(null);
        }

        public boolean isToggled(String key) {
            return toggled.getOrDefault(key, true);
        }

        public void toggle(String key, boolean enabled) {
            if (key == null || key.isEmpty()) return;
            if (enabled) {
                // Turn off all other entries with the same key
                for (String k : toggled.keySet()) {
                    if (k.equals(key)) continue;
                    if (k.equalsIgnoreCase(key)) toggled.put(k, false);
                }
            }
            toggled.put(key, enabled);
            if (onChange != null) onChange.accept(null);
        }

        public void loadFromJson(JsonObject json) {
            if (json.has(valuesKey)) {
                Map<String, V> loaded = GSON.fromJson(json.get(valuesKey), valueType);
                values.clear();
                values.putAll(loaded);
            }
            if (json.has(toggledKey)) {
                Map<String, Boolean> loaded = GSON.fromJson(json.get(toggledKey), toggledType);
                toggled.clear();
                toggled.putAll(loaded);
            }
            toggled.keySet().removeIf(k -> !values.containsKey(k));
        }

        public void saveToJson(JsonObject json) {
            json.add(valuesKey, GSON.toJsonTree(values));
            json.add(toggledKey, GSON.toJsonTree(toggled));
        }
    }

    public static class SimpleConfigSection<V> {
        private final Map<String, V> values;
        private final String valuesKey;
        private final Type valueType;
        private final Consumer<Void> onChange;

        public SimpleConfigSection(String valuesKey, Type valueType, Consumer<Void> onChange) {
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

    // Section instances
    public static final ConfigSection<String> commandAliases =
        new ConfigSection<>("commandAliases", "toggledCommands",
            new TypeToken<Map<String, String>>(){}.getType(),
            new TypeToken<Map<String, Boolean>>(){}.getType(),
            v -> { save(); CommandAlias.refreshCache(); });

    public static final ConfigSection<String> chatReplacements =
        new ConfigSection<>("chatReplacements", "toggledChatReplacements",
            new TypeToken<Map<String, String>>(){}.getType(),
            new TypeToken<Map<String, Boolean>>(){}.getType(),
            v -> { save(); ChatReplacement.refreshCache(); });

    public static final ConfigSection<String> keybinds =
        new ConfigSection<>("keybinds", "toggledKeybinds",
            new TypeToken<Map<String, String>>(){}.getType(),
            new TypeToken<Map<String, Boolean>>(){}.getType(),
            v -> { save(); KeyShortcut.refreshCache();});

    // --- Mod Settings ---
    public static final SimpleConfigSection<Object> settings =
        new SimpleConfigSection<>("settings",
            new TypeToken<Map<String, Object>>(){}.getType(),
            v -> save());

    // --- Mod Keybinds ---
    public static final SimpleConfigSection<String> modKeys =
        new SimpleConfigSection<>("modKeys",
            new TypeToken<Map<String, String>>(){}.getType(),
            v -> save());

    // --- HUD ---
    public static final SimpleConfigSection<Object> hud =
        new SimpleConfigSection<>("hud",
            new TypeToken<Map<String, Object>>(){}.getType(),
            v -> save());


    // --- Config keys ---
    public static final String HUD_PING_X = "pingHudX";
    public static final String HUD_PING_Y = "pingHudY";
    public static final String HUD_PING_ENABLED = "pingHud";
    public static final String HUD_PING_SIZE = "pingHudSize";
    public static final String HUD_PING_COLOR = "pingHudColor";
    public static final String HUD_TIMER_X = "timerHudX";
    public static final String HUD_TIMER_Y = "timerHudY";
    public static final String HUD_TIMER_ENABLED = "timerHud";
    public static final String HUD_TIMER_SIZE = "timerHudSize";
    public static final String HUD_TIMER_COLOR = "timerHudColor";
    public static final String RENDER_COORD_COLOR = "renderCoordsColor";
    private static final String CUSTOM_PARTICLE_COLOR_INDEX = "customParticleColorIndex";
    private static final String CUSTOM_PARTICLE_MODE = "customParticleMode";
    private static final String SKIP_F5 = "skipPerspective";
    private static final String CLEAN_HYPE = "cleanHype";
    private static final String MUTE_PHANTOM = "mutePhantom";
    private static final String RENDER_COORDS = "renderCoords";
    private static final String BEACON_ALARM = "beaconAlarm";

    // Generalized HUD position getters/setters
    public static int getHudX(String hudKey, int defaultX) {
        Object value = hud.getValues().getOrDefault(hudKey + "X", defaultX);
        return value instanceof Number n ? n.intValue() : defaultX;
    }
    public static int getHudY(String hudKey, int defaultY) {
        Object value = hud.getValues().getOrDefault(hudKey + "Y", defaultY);
        return value instanceof Number n ? n.intValue() : defaultY;
    }
    public static void setHudX(String hudKey, int x) {
        if (!hud.getValues().containsKey(hudKey + "X")) {
            hud.set(hudKey + "X", 5);
        }
        hud.set(hudKey + "X", x);
        save();
    }
    public static void setHudY(String hudKey, int y) {
        if (!hud.getValues().containsKey(hudKey + "Y")) {
            hud.set(hudKey + "Y", 5);
        }
        hud.set(hudKey + "Y", y);
        save();
    }

    public static int getHudSize(String hudKey, int defaultSize) {
        Object value = hud.getValues().getOrDefault(hudKey + "Size", defaultSize);
        return value instanceof Number n ? n.intValue() : defaultSize;
    }
    public static void setHudSize(String hudKey, int size) {
        hud.set(hudKey + "Size", size);
        save();
    }
    public static int getHudColor(String hudKey, int defaultColor) {
        Object value = hud.getValues().getOrDefault(hudKey + "Color", defaultColor);
        return value instanceof Number n ? n.intValue() : defaultColor;
    }
    public static void setHudColor(String hudKey, int color) {
        hud.set(hudKey + "Color", color);
        save();
    }

    static {
        File root = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons");
        CONFIG_FILE = new File(root, "fishyaddons.json");
        BACKUP_DIR = new File(root, "backup");
        BACKUP_FILE = new File(BACKUP_DIR, "fishyaddons.json");
    }

    private static boolean firstLoad = false;
    private static boolean recreatedConfig = false;
    private static boolean restoredConfig = false;
    private static boolean configChanged = false;

    // Custom RGB values for particles
    private static float customRed = 1.0f;
    private static float customGreen = 0.0f;
    private static float customBlue = 0.0f;

    public static boolean isFirstLoad() { return firstLoad; }
    public static boolean isRecreated() { return recreatedConfig; }
    public static boolean isRestored() { return restoredConfig; }

    public static void resetFlags() {
        recreatedConfig = false;
        restoredConfig = false;
        firstLoad = false;
    }

    public static void init() {
        CONFIG_FILE.getParentFile().mkdirs();
        BACKUP_DIR.mkdirs();
        load();

        if (firstLoad) {
            commandAliases.set("/m7", "/joininstance MASTER_CATACOMBS_FLOOR_SEVEN");
            keybinds.set("MOUSE3", "/pets");
            keybinds.set("GLFW_KEY_B", "/wardrobe");
            chatReplacements.set(":cat:", "ᗢᘏᓗ");            
            chatReplacements.set(":hi:", "ඞ");
            chatReplacements.set("heii", "Any string will be replaced one to one");
            settings.set("fishyLava", false);
            settings.set(CUSTOM_PARTICLE_COLOR_INDEX, Integer.valueOf(1));
            settings.set(SKIP_F5, false);
            settings.set(CLEAN_HYPE, false);
            settings.set(MUTE_PHANTOM, false);
            settings.set(RENDER_COORDS, false);
            settings.set(CUSTOM_PARTICLE_MODE, "preset");
            settings.set(BEACON_ALARM, false);
            settings.set(HUD_PING_ENABLED, false);
            settings.set(HUD_TIMER_ENABLED, false);
            hud.set(HUD_PING_X, 5);
            hud.set(HUD_PING_Y, 5);
            hud.set(HUD_PING_SIZE, 12);
            hud.set(HUD_PING_COLOR, 1.5649516E7);
            hud.set(HUD_TIMER_X, 5);
            hud.set(HUD_TIMER_Y, 20);
            hud.set(HUD_TIMER_SIZE, 12);
            hud.set(HUD_TIMER_COLOR, 1.5649516E7);
            hud.set(RENDER_COORD_COLOR, 0xFF00FFFF);
            modKeys.set("lockKey", "GLFW_KEY_L");
            save();
        }
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
            System.err.println("[FishyConfig] Config file does not exist. Checking for backup...");
            loadOrRestore();
            firstLoad = true;
            return;
        }

        try (Reader reader = new FileReader(CONFIG_FILE)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            if (json == null || !validate(json)) {
                System.err.println("[FishyConfig] Invalid config detected. Attempting restore...");
                loadOrRestore();
                return;
            }

            commandAliases.loadFromJson(json);
            chatReplacements.loadFromJson(json);
            keybinds.loadFromJson(json);
            settings.loadFromJson(json);
            modKeys.loadFromJson(json);
            hud.loadFromJson(json);

            // --- Load custom RGB ---
            if (json.has("customRed")) customRed = json.get("customRed").getAsFloat();
            if (json.has("customGreen")) customGreen = json.get("customGreen").getAsFloat();
            if (json.has("customBlue")) customBlue = json.get("customBlue").getAsFloat();

        } catch (JsonSyntaxException | JsonIOException e) {
            System.err.println("[FishyConfig] Malformed JSON: " + e.getMessage());
            loadOrRestore();
        } catch (IOException e) {
            System.err.println("[FishyConfig] Failed to read config: " + e.getMessage());
            loadOrRestore();
        }
    }

    public static synchronized void save() {
        try (Writer writer = new FileWriter(CONFIG_FILE)) {
            JsonObject json = new JsonObject();
            commandAliases.saveToJson(json);
            chatReplacements.saveToJson(json);
            keybinds.saveToJson(json);
            settings.saveToJson(json);
            modKeys.saveToJson(json);
            hud.saveToJson(json);

            // --- Save custom RGB ---
            json.addProperty("customRed", customRed);
            json.addProperty("customGreen", customGreen);
            json.addProperty("customBlue", customBlue);

            GSON.toJson(json, writer);
        } catch (IOException e) {
            System.err.println("[FishyConfig] Failed to save config: " + e.getMessage());
        }
    }

    public static void saveBackup() {
        try {
            if (CONFIG_FILE.exists()) {
                Files.copy(CONFIG_FILE.toPath(), BACKUP_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[FishyConfig] Failed to create backup: " + e.getMessage());
        }
    }

    private static boolean validate(JsonObject json) {
        return json.has("commandAliases") && json.has("toggledCommands") 
            && json.has("chatReplacements") && json.has("toggledChatReplacements")
            && json.has("keybinds") && json.has("toggledKeybinds")
            && json.has("settings") && json.has("modKeys");
    }

    private static void loadOrRestore() {
        if (BACKUP_FILE.exists()) {
            System.err.println("[FishyConfig] Restoring from backup...");
            try {
                Files.copy(BACKUP_FILE.toPath(), CONFIG_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                load();
                restoredConfig = true;
                return;
            } catch (IOException e) {
                System.err.println("[FishyConfig] Backup restore failed: " + e.getMessage());
            }
        }

        System.err.println("[FishyConfig] No backup found. Creating default config...");
        save();
        recreatedConfig = true;
    }

    // -- Type-specific wrappers ----

    // Aliases
    public static Map<String, String> getCommandAliases() { return commandAliases.getValues(); }
    public static void setCommandAlias(String alias, String command) { commandAliases.set(alias, command); }
    public static void removeCommandAlias(String alias) { commandAliases.remove(alias); }
    public static boolean isCommandToggled(String alias) { return commandAliases.isToggled(alias); }
    public static void toggleCommand(String alias, boolean enabled) { commandAliases.toggle(alias, enabled); }

    // Chat replacements
    public static Map<String, String> getChatReplacements() { return chatReplacements.getValues(); }
    public static void setChatReplacement(String key, String value) { chatReplacements.set(key, value); }
    public static void removeChatReplacement(String key) { chatReplacements.remove(key); }
    public static boolean isChatReplacementToggled(String key) { return chatReplacements.isToggled(key); }
    public static void toggleChatReplacement(String key, boolean enabled) { chatReplacements.toggle(key, enabled); }

    // Keybinds
    public static Map<String, String> getKeybinds() { return keybinds.getValues(); }
    public static void setKeybind(String key, String value) { keybinds.set(key, value); }
    public static void removeKeybind(String key) { keybinds.remove(key); }
    public static boolean isKeybindToggled(String key) { return keybinds.isToggled(key); }
    public static void toggleKeybind(String key, boolean enabled) { keybinds.toggle(key, enabled); }


    // --- Feature Settings --- 

    // Mod Keybinds
    public static String getLockKey() {
        return modKeys.getValues().getOrDefault("lockKey", "GLFW_KEY_L");
    }

    public static void setLockKey(String key) {
        modKeys.set("lockKey", key);
        save();
    }

    // --- Custom Redstone Particle API ---

    public static int getCustomParticleColorIndex() {
        Object value = settings.getValues().get(CUSTOM_PARTICLE_COLOR_INDEX);
        return value instanceof Number number ? number.intValue() : 1;
    }

    public static void setCustomParticleColorIndex(int index) {
        settings.set(CUSTOM_PARTICLE_COLOR_INDEX, index);
        save();
    }

    public static float[] getCustomParticleRGB() {
        return new float[]{customRed, customGreen, customBlue};
    }

    public static void setCustomParticleRGB(float r, float g, float b) {
        customRed = r;
        customGreen = g;
        customBlue = b;
        save();
    }

    public static String getParticleColorMode() {
        Object value = settings.getValues().getOrDefault(CUSTOM_PARTICLE_MODE, "preset");
        return value instanceof String string ? string : "preset";
    }

    public static void setParticleColorMode(String mode) {
        settings.set(CUSTOM_PARTICLE_MODE, mode);
        save();
    }    

    // ---  Generalized Methods for Settings ---
    // Boolean

    public static boolean getState(String key, boolean def) {
        Object v = settings.getValues().getOrDefault(key, def);
        return v instanceof Boolean b ? b : def;
    }    

    public static void enable(String key, boolean enabled) {
        settings.set(key, enabled);
        save();
    }

    public static void disable(String key) {
        settings.set(key, false);
        save();
    }

    public static void toggle(String key, boolean def) {
        boolean current = getState(key, def);
        enable(key, !current);
    }

    //get Integer value
    public static int getInt(String key) {
        Object v = settings.getValues().get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }

    public static void setInt(String key, int value) {
        settings.set(key, value);
        save();
    }
}
