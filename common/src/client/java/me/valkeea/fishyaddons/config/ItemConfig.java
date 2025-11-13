package me.valkeea.fishyaddons.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.feature.item.safeguard.BlacklistManager;
import me.valkeea.fishyaddons.feature.skyblock.GuiIcons;
import me.valkeea.fishyaddons.util.JsonUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ItemConfig {
    private ItemConfig() {}
    private static final File CONFIG_FILE;
    private static final File BACKUP_DIR;
    private static final File BACKUP_FILE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String LOCKED_PREFIX = "locked_";
    private static final String BOUND_PREFIX = "bound_";
    private static final String ITEMSTACK_PREFIX = "itemstack_";
    private static final String EQUIPMENT_ITEMSTACKS_KEY = "equipmentItemStacks";
    private static final String GUIICONS_SCREEN_NAMES = "guiicons_screenNames";
    private static final String GUIICONS_SCREEN_SLOT_MAP = "guiicons_screenSlotMap";    

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

        public void clear() {
            values.clear();
            if (onChange != null) onChange.accept(null);
        }

        public void loadFromJson(JsonObject json) {
            if (json.has(valuesKey)) {
                JsonElement element = json.get(valuesKey);
                Map<String, V> loaded = GSON.fromJson(element, valueType);
                if (loaded != null) {
                    values.clear();
                    values.putAll(loaded);
                }
            }
        }

        public void saveToJson(JsonObject json) {
            json.add(valuesKey, GSON.toJsonTree(values));
        }
    }

    // Section instances
    public static final SimpleConfigSection<Object> settings =
        new SimpleConfigSection<>("settings",
            new TypeToken<Map<String, Object>>(){}.getType(),
            v -> save());

    public static final SimpleConfigSection<String> protectedItems =
        new SimpleConfigSection<>(Key.PROTECTED_UUIDS,
            new TypeToken<Map<String, String>>(){}.getType(),
            v -> save());

    public static final SimpleConfigSection<Integer> slotData =
        new SimpleConfigSection<>("slotData",
            new TypeToken<Map<String, Integer>>(){}.getType(),
            v -> save());

    public static final SimpleConfigSection<String> equipmentData =
        new SimpleConfigSection<>("equipmentData",
            new TypeToken<Map<String, String>>(){}.getType(),
            v -> save());

    public static final SimpleConfigSection<Object> blacklistData =
        new SimpleConfigSection<>(Key.BLACKLIST,
            new TypeToken<List<Map<String, Object>>>(){}.getType(),
            v -> save());

    static {
        File root = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons");
        CONFIG_FILE = new File(root, "fishyitems.json");
        BACKUP_DIR = new File(root, "backup");
        BACKUP_FILE = new File(BACKUP_DIR, "fishyitems.json");
    }

    private static boolean firstLoad = false;
    private static boolean recreatedConfig = false;
    private static boolean restoredConfig = false;
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
        CONFIG_FILE.getParentFile().mkdirs();
        BACKUP_DIR.mkdirs();
        load();

        if (firstLoad) {
            // Set default values
            settings.set(Key.SELL_PROTECTION_ENABLED, true);
            settings.set(Key.TOOLTIP_ENABLED, true);
            settings.set(Key.PROTECT_TRIGGER_ENABLED, true);
            settings.set(Key.PROTECT_NOTI_ENABLED, true);
            settings.set(Key.LOCK_TRIGGER_ENABLED, true);
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
            System.err.println("[ItemConfig] Config file does not exist. Checking for backup...");
            loadOrRestore();
            firstLoad = true;
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(CONFIG_FILE), java.nio.charset.StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            if (!validate(json)) {
                System.err.println("[ItemConfig] Invalid config detected. Attempting to restore from backup...");
                loadOrRestore();
                return;
            }

            settings.loadFromJson(json);

            if (json.has(Key.PROTECTED_UUIDS)) {
                JsonElement element = json.get(Key.PROTECTED_UUIDS);

                if (element.isJsonObject()) {
                    protectedItems.loadFromJson(json);
                    
                } else if (element.isJsonArray()) {
                    JsonArray array = element.getAsJsonArray();
                    for (JsonElement item : array) {
                        if (item.isJsonPrimitive()) {
                            String uuid = item.getAsString();
                            protectedItems.set(uuid, "Protected Item");
                        }
                    }
                }
            }

            loadSlotData(json);
            loadEquipmentData(json);
            loadBlacklistData(json);
            loadGuiIconsConfig(json);

        } catch (JsonSyntaxException | JsonIOException e) {
            System.err.println("[ItemConfig] JSON parsing error: " + e.getMessage());
            loadOrRestore();
        } catch (IOException e) {
            System.err.println("[ItemConfig] IO error loading config: " + e.getMessage());
            loadOrRestore();
        }
    }


    private static void loadSlotData(JsonObject json) {
        loadLockedSlots(json);
        loadBoundSlots(json);
    }

    private static void loadLockedSlots(JsonObject json) {
        if (json.has(Key.LOCKED_SLOTS)) {
            JsonElement element = json.get(Key.LOCKED_SLOTS);
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                for (JsonElement item : array) {
                    if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isNumber()) {
                        int slot = item.getAsInt();
                        slotData.set(LOCKED_PREFIX + slot, slot);
                    }
                }
            }
        }
    }

    private static void loadBoundSlots(JsonObject json) {
        if (json.has(Key.BOUND_SLOTS)) {
            JsonElement element = json.get(Key.BOUND_SLOTS);
            if (element.isJsonObject()) {
                JsonObject boundObj = element.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : boundObj.entrySet()) {
                    try {
                        int from = Integer.parseInt(entry.getKey());
                        int to = entry.getValue().getAsInt();
                        slotData.set(BOUND_PREFIX + from, to);
                        slotData.set(BOUND_PREFIX + to, from); // Bidirectional binding
                    } catch (NumberFormatException e) {
                        System.err.println("[ItemConfig] Invalid bound slot key: " + entry.getKey());
                    }
                }
            }
        }
    }

    private static void loadEquipmentData(JsonObject json) {
        if (json.has(EQUIPMENT_ITEMSTACKS_KEY)) {
            JsonElement element = json.get(EQUIPMENT_ITEMSTACKS_KEY);
            if (element.isJsonObject()) {
                JsonObject itemStacksObj = element.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : itemStacksObj.entrySet()) {
                    try {
                        int slot = Integer.parseInt(entry.getKey());
                        String itemStackData = entry.getValue().getAsString();
                        equipmentData.set(ITEMSTACK_PREFIX + slot, itemStackData);
                    } catch (NumberFormatException e) {
                        System.err.println("[ItemConfig] Invalid equipment itemstack slot: " + entry.getKey());
                    }
                }
            }
        }
    }

    private static void loadBlacklistData(JsonObject json) {
        if (json.has(Key.BLACKLIST)) {
            JsonElement element = json.get(Key.BLACKLIST);
            if (element.isJsonArray()) {
                List<Map<String, Object>> entries = parseBlacklistArray(element.getAsJsonArray());
                BlacklistManager.loadUserBlacklistFromJson(entries);
            }
        }
    }

    private static List<Map<String, Object>> parseBlacklistArray(JsonArray array) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (JsonElement item : array) {
            if (item.isJsonObject()) {
                Map<String, Object> entry = parseBlacklistObject(item.getAsJsonObject());
                entries.add(entry);
            }
        }
        return entries;
    }

    private static Map<String, Object> parseBlacklistObject(JsonObject obj) {
        Map<String, Object> entry = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            JsonElement value = e.getValue();
            if (value.isJsonPrimitive()) {
                JsonPrimitive primitive = value.getAsJsonPrimitive();
                if (primitive.isString()) {
                    entry.put(e.getKey(), primitive.getAsString());
                } else if (primitive.isNumber()) {
                    entry.put(e.getKey(), primitive.getAsNumber());
                } else if (primitive.isBoolean()) {
                    entry.put(e.getKey(), primitive.getAsBoolean());
                }
            }
        }
        return entry;
    }

    private static void loadGuiIconsConfig(JsonObject json) {
        // screenNames
        if (json.has(GUIICONS_SCREEN_NAMES)) {
            JsonElement element = json.get(GUIICONS_SCREEN_NAMES);
            Set<String> names = GSON.fromJson(element, new TypeToken<Set<String>>(){}.getType());
            if (names != null) {
                GuiIcons.setScreenNames(names);
            }
        }
        // screenSlotMap
        if (json.has(GUIICONS_SCREEN_SLOT_MAP)) {
            JsonElement element = json.get(GUIICONS_SCREEN_SLOT_MAP);
            Map<String, Set<Integer>> map = GSON.fromJson(element, new TypeToken<Map<String, Set<Integer>>>(){}.getType());
            if (map != null) {
                GuiIcons.setScreenSlotMap(map);
            }
        }
    }    

    public static synchronized void save() {
        JsonObject json = new JsonObject();

        settings.saveToJson(json);
        protectedItems.saveToJson(json);
        saveSlotData(json);
        saveEquipmentData(json);
        saveGuiIconsConfig(json);

        List<Map<String, Object>> serializedBlacklist = BlacklistManager.getUserBlacklistAsJson();
        json.add(Key.BLACKLIST, GSON.toJsonTree(serializedBlacklist));

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE), java.nio.charset.StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            System.err.println("[ItemConfig] Failed to save config: " + e.getMessage());
        }
    }

    private static void saveSlotData(JsonObject json) {
        List<Integer> lockedSlots = new ArrayList<>();
        Map<Integer, Integer> boundSlots = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : slotData.getValues().entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            
            if (key.startsWith(LOCKED_PREFIX)) {
                lockedSlots.add(value);
            } else if (key.startsWith(BOUND_PREFIX)) {
                try {
                    int slot = Integer.parseInt(key.substring(BOUND_PREFIX.length()));
                    boundSlots.put(slot, value);
                } catch (NumberFormatException e) {
                    System.err.println("[ItemConfig] Invalid bound slot key format: " + key);
                }
            }
        }

        json.add(Key.LOCKED_SLOTS, GSON.toJsonTree(lockedSlots));

        Map<Integer, Integer> filteredBoundSlots = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : boundSlots.entrySet()) {
            int from = entry.getKey();
            int to = entry.getValue();
            if (from < to) {
                filteredBoundSlots.put(from, to);
            }
        }
        json.add(Key.BOUND_SLOTS, GSON.toJsonTree(filteredBoundSlots));
    }

    private static void saveEquipmentData(JsonObject json) {
        Map<String, String> itemStacks = new HashMap<>();

        for (Map.Entry<String, String> entry : equipmentData.getValues().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith(ITEMSTACK_PREFIX)) {
                String slotStr = key.substring(ITEMSTACK_PREFIX.length());
                itemStacks.put(slotStr, value);
            }
        }

        if (!itemStacks.isEmpty()) {
            json.add(EQUIPMENT_ITEMSTACKS_KEY, GSON.toJsonTree(itemStacks));
        }
    }

    public static void saveBackup() {
        try {
            if (CONFIG_FILE.exists()) {
                Files.copy(CONFIG_FILE.toPath(), BACKUP_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("[ItemConfig] Failed to save backup: " + e.getMessage());
        }
    }

    private static boolean validate(JsonObject json) {
        return json != null && json.size() > 0;
    }

    private static void loadOrRestore() {
        if (BACKUP_FILE.exists()) {
            System.err.println("[ItemConfig] Restoring from backup...");
            try {
                Files.copy(BACKUP_FILE.toPath(), CONFIG_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                load();
                restoredConfig = true;
                return;
            } catch (IOException e) {
                System.err.println("[ItemConfig] Failed to restore from backup: " + e.getMessage());
            }
        }

        System.err.println("[ItemConfig] No backup found. Creating default config...");
        save();
        recreatedConfig = true;
    }

    private static void saveGuiIconsConfig(JsonObject json) {
        Set<String> names = GuiIcons.getScreenNames();
        Map<String, Set<Integer>> map = GuiIcons.getScreenSlotMap();
        json.add(GUIICONS_SCREEN_NAMES, GSON.toJsonTree(names));
        json.add(GUIICONS_SCREEN_SLOT_MAP, GSON.toJsonTree(map));
    }    

    // --- Generalized Methods for Settings ---

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

    // Integer
    public static int getInt(String key, int def) {
        Object v = settings.getValues().getOrDefault(key, def);
        return v instanceof Number n ? n.intValue() : def;
    }

    public static void setInt(String key, int value) {
        settings.set(key, value);
        save();
    }

    // String
    public static String getString(String key, String def) {
        Object v = settings.getValues().getOrDefault(key, def);
        return v instanceof String str ? str : def;
    }

    public static void setString(String key, String value) {
        settings.set(key, value);
        save();
    }

    // Float
    public static float getFloat(String key, float def) {
        Object v = settings.getValues().getOrDefault(key, def);
        return v instanceof Number n ? n.floatValue() : def;
    }

    public static void setFloat(String key, float value) {
        settings.set(key, value);
        save();
    }

    // --- Item Protection API (Legacy UI) ---
    public static boolean isSellProtectionEnabled() {
        return getState(Key.SELL_PROTECTION_ENABLED, true);
    }

    public static void setSellProtectionEnabled(boolean enabled) {
        enable(Key.SELL_PROTECTION_ENABLED, enabled);
    }

    public static boolean isTooltipEnabled() {
        return getState(Key.TOOLTIP_ENABLED, true);
    }

    public static void setTooltipEnabled(boolean enabled) {
        enable(Key.TOOLTIP_ENABLED, enabled);
    }

    public static boolean isProtectTriggerEnabled() {
        return getState(Key.PROTECT_TRIGGER_ENABLED, true);
    }

    public static void setProtectTriggerEnabled(boolean enabled) {
        enable(Key.PROTECT_TRIGGER_ENABLED, enabled);
    }

    public static boolean isProtectNotiEnabled() {
        return getState(Key.PROTECT_NOTI_ENABLED, true);
    }

    public static void setProtectNotiEnabled(boolean enabled) {
        enable(Key.PROTECT_NOTI_ENABLED, enabled);
    }

    public static boolean isLockTriggerEnabled() {
        return getState(Key.LOCK_TRIGGER_ENABLED, true);
    }

    public static void setLockTriggerEnabled(boolean enabled) {
        enable(Key.LOCK_TRIGGER_ENABLED, enabled);
    }

    public static synchronized void addUUID(String uuid, Text displayName) {
        String serialized = JsonUtil.serializeText(displayName);
        protectedItems.set(uuid, serialized);
        save();
    }

    public static synchronized void removeUUID(String uuid) {
        protectedItems.remove(uuid);
        save();
    }

    public static synchronized void clearAll() {
        protectedItems.clear();
        save();
    }

    public static synchronized boolean isProtected(String uuid) {
        return protectedItems.getValues().containsKey(uuid);
    }

    public static synchronized Text getDisplayName(String uuid) {
        String serialized = protectedItems.getValues().get(uuid);
        return serialized != null ? JsonUtil.deserializeText(serialized) : null;
    }

    public static synchronized Map<String, String> getProtectedUUIDs() {
        return new HashMap<>(protectedItems.getValues());
    }

    // --- Slot Locking ---
    public static synchronized boolean isSlotLocked(int slot) {
        return slotData.getValues().containsKey(LOCKED_PREFIX + slot);
    }

    public static synchronized void toggleSlotLock(int slot) {
        String key = LOCKED_PREFIX + slot;
        if (slotData.getValues().containsKey(key)) {
            slotData.remove(key);
        } else {
            slotData.set(key, slot);
        }
        save();
    }

    // --- Slot Binding ---
    public static synchronized boolean areSlotsBound(int slotA, int slotB) {
        Integer boundSlotA = slotData.getValues().get(BOUND_PREFIX + slotA);
        return boundSlotA != null && boundSlotA == slotB;
    }

    public static synchronized boolean isSlotBound(int slot) {
        return slotData.getValues().containsKey(BOUND_PREFIX + slot);
    }

    public static synchronized int getBoundSlot(int slot) {
        Integer bound = slotData.getValues().get(BOUND_PREFIX + slot);
        return bound != null ? bound : -1;
    }

    public static synchronized void bindSlots(int slotA, int slotB) {
        slotData.set(BOUND_PREFIX + slotA, slotB);
        slotData.set(BOUND_PREFIX + slotB, slotA);
        save();
    }

    public static synchronized void unbindSlots(int slotA, int slotB) {
        slotData.remove(BOUND_PREFIX + slotA);
        slotData.remove(BOUND_PREFIX + slotB);
        save();
    }

    // --- Eq ItemStacks ---

    public static synchronized void clearEquipmentSlot(int slot) {
        equipmentData.remove(ITEMSTACK_PREFIX + slot);
        save();
    }

    public static synchronized void setEqItemStack(int slot, String serializedItemStack) {
        String key = ITEMSTACK_PREFIX + slot;
        if (serializedItemStack == null || serializedItemStack.isEmpty()) {
            equipmentData.remove(key);
        } else {
            equipmentData.set(key, serializedItemStack);
        }
        save();
    }

    public static synchronized String getEqItemStack(int slot) {
        return equipmentData.getValues().get(ITEMSTACK_PREFIX + slot);
    }

    public static synchronized Map<Integer, String> getAllEqItemStacks() {
        Map<Integer, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : equipmentData.getValues().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(ITEMSTACK_PREFIX)) {
                try {
                    int slot = Integer.parseInt(key.substring(ITEMSTACK_PREFIX.length()));
                    result.put(slot, entry.getValue());
                } catch (NumberFormatException e) {
                    System.err.println("[ItemConfig] Invalid itemstack slot key: " + key);
                }
            }
        }
        return result;
    }

    public static void saveGuiIcons() {
        save();
    }
}
