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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.feature.qol.ChatReplacement;
import me.valkeea.fishyaddons.feature.qol.CommandAlias;
import me.valkeea.fishyaddons.feature.qol.KeyShortcut;
import net.minecraft.client.MinecraftClient;

public class FishyConfig {
    private FishyConfig() {}
    private static final File CONFIG_FILE;
    private static final File BACKUP_DIR;
    private static final File BACKUP_FILE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String RED = "customRed";
    private static final String GREEN = "customGreen";
    private static final String BLUE = "customBlue";
    private static final String PRESET = "preset";

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
            v -> { save(); CommandAlias.refresh(); });

    public static final ConfigSection<String> chatReplacements =
        new ConfigSection<>("chatReplacements", "toggledChatReplacements",
            new TypeToken<Map<String, String>>(){}.getType(),
            new TypeToken<Map<String, Boolean>>(){}.getType(),
            v -> { save(); ChatReplacement.refresh(); });

    public static final ConfigSection<String> keybinds =
        new ConfigSection<>("keybinds", "toggledKeybinds",
            new TypeToken<Map<String, String>>(){}.getType(),
            new TypeToken<Map<String, Boolean>>(){}.getType(),
            v -> { save(); KeyShortcut.refresh();});

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

    // --- FaColors ---
    public static final SimpleConfigSection<Integer> faColors =
        new SimpleConfigSection<>("faColors",
            new com.google.gson.reflect.TypeToken<Map<String, Integer>>(){}.getType(),
            v -> save());

    // Generalized HUD position getters/setters
    public static int getHudX(String hudKey, int defaultX) {
        Object value = hud.getValues().getOrDefault(hudKey + "X", defaultX);
        value = Math.clamp(value instanceof Number n ? n.intValue() : defaultX, 0, MinecraftClient.getInstance().getWindow().getWidth());
        return value instanceof Number n ? n.intValue() : defaultX;
    }

    public static int getHudY(String hudKey, int defaultY) {
        Object value = hud.getValues().getOrDefault(hudKey + "Y", defaultY);
        value = Math.clamp(value instanceof Number n ? n.intValue() : defaultY, 0, MinecraftClient.getInstance().getWindow().getHeight());
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
        return value instanceof Number n ? n.intValue() | 0xFF000000 : defaultColor;
    }

    public static void setHudColor(String hudKey, int color) {
        hud.set(hudKey + "Color", color);
        save();
    }

    public static boolean getHudOutline(String hudKey, boolean outline) {
        Object value = hud.getValues().getOrDefault(hudKey + "Outline", outline);
        return value instanceof Boolean b ? b : outline;
    }

    public static void setHudOutline(String hudKey, boolean outline) {
        hud.set(hudKey + "Outline", outline);
        save();
    }

    public static boolean getHudBg(String hudKey, boolean bg) {
        Object value = hud.getValues().getOrDefault(hudKey + "Bg", bg);
        return value instanceof Boolean b ? b : bg;
    }

    public static void setHudBg(String hudKey, boolean bg) {
        hud.set(hudKey + "Bg", bg);
        save();
    }

    public static Map<String, Integer> getFaC() {
        return faColors.getValues();
    }

    public static void setFaC(String key, int color) {
        faColors.set(key, color);
        save();
    }

    public static void removeFaC(String key) {
        faColors.remove(key);
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
            Key.setDefaults(commandAliases, chatReplacements, settings, hud, modKeys);
            save();
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
            chatAlerts.loadFromJson(json);
            faColors.loadFromJson(json);

            // --- Load custom RGB ---
            if (json.has(RED)) customRed = json.get(RED).getAsFloat();
            if (json.has(GREEN)) customGreen = json.get(GREEN).getAsFloat();
            if (json.has(BLUE)) customBlue = json.get(BLUE).getAsFloat();

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
            chatAlerts.saveToJson(json);
            faColors.saveToJson(json);

            // --- Save custom RGB ---
            json.addProperty(RED, customRed);
            json.addProperty(GREEN, customGreen);
            json.addProperty(BLUE, customBlue);

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
                TrackerProfiles.tryRestore();
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
        return modKeys.getValues().getOrDefault(Key.MOD_KEY_LOCK, "GLFW_KEY_L");
    }

    public static void setLockKey(String key) {
        modKeys.set(Key.MOD_KEY_LOCK, key);
        save();
    }

    public static String getKeyString(String modKey) {
        return modKeys.getValues().getOrDefault(modKey, "NONE");
    }

    public static void setKeyString(String modKey, String key) {
        modKeys.set(modKey, key);
        save();
    }

    // --- Custom Redstone Particle API ---

    public static int getCustomParticleColorIndex() {
        Object value = settings.getValues().get(Key.CUSTOM_PARTICLE_COLOR_INDEX);
        return value instanceof Number number ? number.intValue() : 1;
    }

    public static void setCustomParticleColorIndex(int index) {
        settings.set(Key.CUSTOM_PARTICLE_COLOR_INDEX, index);
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
        Object value = settings.getValues().getOrDefault(Key.CUSTOM_PARTICLE_MODE, PRESET);
        return value instanceof String string ? string : PRESET;
    }

    public static void setParticleColorMode(String mode) {
        settings.set(Key.CUSTOM_PARTICLE_MODE, mode);
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

    // get Integer value
    public static int getInt(String key) {
        Object v = settings.getValues().get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }

    public static int getInt(String key, int def) {
        Object v = settings.getValues().getOrDefault(key, def);
        if (!(v instanceof Number)) {
            settings.set(key, def);
            save();
            return def;
        }
        return ((Number) v).intValue();
    }

    public static void setInt(String key, int value) {
        settings.set(key, value);
        save();
    }

    // get String value
    public static String getString(String key, String def) {
        Object v = settings.getValues().getOrDefault(key, def);
        return v instanceof String str ? str : def;
    }

    public static void setString(String key, String value) {
        settings.set(key, value);
        save();
    }

    public static float getFloat(String key, float def) {
        Object v = settings.getValues().getOrDefault(key, def);
        return v instanceof Number n ? n.floatValue() : def;
    }

    public static void setFloat(String key, float value) {
        settings.set(key, value);
        save();
    }

    // --- Alert Data ---
    public static class AlertData {
        private String msg;
        private String onscreen;
        private int color;
        private String soundId;
        private float volume;
        private boolean toggled;
        private boolean startsWith;

        public AlertData() {
            this("", "", 0xFF6DE6B5, "", 1.0F, true, false);
        }

        public AlertData(String msg, String onscreen, int color, String soundId, float volume, boolean toggled, boolean startsWith) {
            this.msg = msg;
            this.onscreen = onscreen;
            this.color = color;
            this.soundId = soundId;
            this.volume = Math.clamp(volume, 0.0f, 10.0f);
            this.toggled = toggled;
            this.startsWith = startsWith;
        }

        public void setMsg(String msg) { this.msg = msg; }        
        public String getMsg() { return msg; }
        public String getOnscreen() { return onscreen; }

        public int getColor() { 
            if ((color & 0xFF000000) == 0) {
                color |= 0xFF000000;
            }
            return color; 
        }
        
        public String getSoundId() { return soundId; }
        public float getVolume() { return volume; }
        public boolean isToggled() { return toggled; }  
        public void setToggled(boolean toggled) { this.toggled = toggled; }
        public boolean isStartsWith() { return startsWith; }
        public void setStartsWith(boolean startsWith) { this.startsWith = startsWith; }
        public void setOnscreen(String onscreen) { this.onscreen = onscreen; }
        public void setColor(int color) { this.color = color; }
        public void setSoundId(String soundId) { this.soundId = soundId; }
        public void setVolume(float volume) { this.volume = Math.clamp(volume, 0.0f, 10.0f); }
    }

    // --- Section instance for alerts ---
    public static final ConfigSection<AlertData> chatAlerts =
        new ConfigSection<>("chatAlerts", "toggledChatAlerts",
            new com.google.gson.reflect.TypeToken<Map<String, AlertData>>(){}.getType(),
            new com.google.gson.reflect.TypeToken<Map<String, Boolean>>(){}.getType(),
            v -> save()
        );

    public static Map<String, AlertData> getChatAlerts() {
        return chatAlerts.getValues();
    }

    public static void setChatAlert(String key, AlertData data) {
        chatAlerts.set(key, data);
    }

    public static void removeChatAlert(String key) {
        chatAlerts.remove(key);
    }

    public static boolean isChatAlertToggled(String key) {
        AlertData data = chatAlerts.getValues().get(key);
        if (data != null) return data.toggled;
        return chatAlerts.isToggled(key);
    }

    public static boolean isTitleActive(String key) {
        AlertData data = chatAlerts.getValues().get(key);
        return data != null && data.onscreen != null && !data.onscreen.isBlank();
    }

    public static void toggleChatAlert(String key, boolean enabled) {
        AlertData data = chatAlerts.getValues().get(key);
        if (data != null) {
            data.toggled = enabled;
            chatAlerts.set(key, data);
        } else {
            chatAlerts.toggle(key, enabled);
        }
        save();
    }
}
