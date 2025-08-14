package me.valkeea.fishyaddons.config;

import me.valkeea.fishyaddons.safeguard.BlacklistManager;
import me.valkeea.fishyaddons.util.TextFormatUtil;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ItemConfig {
    private ItemConfig() {}
    private static final File CONFIG_FILE;
    private static final File BACKUP_DIR;
    private static final File BACKUP_FILE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // --- Config Options ---

    private static boolean isSellProtectionEnabled = true;
    private static boolean isTooltipEnabled = true;
    private static boolean isProtectTriggerEnabled = true;
    private static boolean isProtectNotiEnabled = true;
    private static boolean isLockTriggerEnabled = true;

    private static final Map<String, String> protectedUUIDs = new HashMap<>();
    private static final Set<Integer> lockedSlots = new HashSet<>();
    private static final Map<Integer, Integer> boundSlots = new HashMap<>();
    
    // --- Equipment Skulls ---
    private static final Map<Integer, String> equipmentSkullTextures = new HashMap<>();
    private static final Map<Integer, String> equipmentSkullItemData = new HashMap<>();


    // --- State/Flags ---
    private static boolean configChanged = false;
    private static boolean initialized = false;
    private static boolean recreatedConfig = false;
    private static boolean restoredConfig = false;

    // --- Keys ---
    private static final String LOCKED_SLOTS_KEY = "lockedSlots";
    private static final String BOUND_SLOTS_KEY = "boundSlots";
    private static final String UUIDS_KEY = "protectedUUIDs";
    private static final String SELL_PROT_KEY = "sellProtectionEnabled";
    private static final String TOOLTIP_KEY = "tooltipEnabled";
    private static final String PROT_TRIGGER_KEY = "protectTriggerEnabled";
    private static final String PROT_NOTI_KEY = "protectNotiEnabled";
    private static final String LOCK_TRIGGER_KEY = "lockTriggerEnabled";
    private static final String BLACKLIST_KEY = "blacklist";
    private static final String EQUIPMENT_SKULL_TEXTURES_KEY = "equipmentSkullTextures";
    private static final String EQUIPMENT_SKULL_ITEMS_KEY = "equipmentSkullItems";

    static {
        File root = new File(MinecraftClient.getInstance().runDirectory, "config/fishyaddons");
        CONFIG_FILE = new File(root, "fishyitems.json");
        BACKUP_DIR = new File(root, "backup");
        BACKUP_FILE = new File(BACKUP_DIR, "fishyitems.json");
    }

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

    // --- Item Protection ---
    public static synchronized void addUUID(String uuid, Text displayName) {
        String serialized = TextFormatUtil.serialize(displayName);
        if (!protectedUUIDs.containsKey(uuid) || !Objects.equals(protectedUUIDs.get(uuid), serialized)) {
            protectedUUIDs.put(uuid, serialized);
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

    public static synchronized Text getDisplayName(String uuid) {
        return TextFormatUtil.deserialize(protectedUUIDs.get(uuid));
    }

    public static synchronized Map<String, String> getProtectedUUIDs() {
        return new HashMap<>(protectedUUIDs);
    }

    public static boolean isSellProtectionEnabled() { return isSellProtectionEnabled; }
    public static void setSellProtectionEnabled(boolean enabled) {
        isSellProtectionEnabled = enabled;
        save();
    }

    public static boolean isProtectTriggerEnabled() { return isProtectTriggerEnabled; }
    public static void setProtectTriggerEnabled(boolean enabled) {
        isProtectTriggerEnabled = enabled;
        markConfigChanged();
    }

    public static boolean isProtectNotiEnabled() { return isProtectNotiEnabled; }
    public static void setProtectNotiEnabled(boolean enabled) {
        isProtectNotiEnabled = enabled;
        markConfigChanged();
    }

    public static boolean isTooltipEnabled() { return isTooltipEnabled; }
    public static void setTooltipEnabled(boolean enabled) {
        isTooltipEnabled = enabled;
        markConfigChanged();
    }

    // ---  Slot Locking ---
    public static boolean isLockTriggerEnabled() { return isLockTriggerEnabled; }
    public static void setLockTriggerEnabled(boolean enabled) {
        isLockTriggerEnabled = enabled;
        markConfigChanged();
    }

    public static synchronized boolean isSlotLocked(int slot) {
        return lockedSlots.contains(slot);
    }

    public static synchronized void toggleSlotLock(int slot) {
        if (lockedSlots.contains(slot)) {
            lockedSlots.remove(slot);
        } else {
            lockedSlots.add(slot);
        }
        markConfigChanged();
    }

    // --- Slot Binding ---
    public static synchronized boolean areSlotsBound(int slotA, int slotB) {
        return boundSlots.get(slotA) != null && boundSlots.get(slotA) == slotB;
    }

    public static synchronized boolean isSlotBound(int slot) {
        return boundSlots.containsKey(slot);
    }

    public static synchronized int getBoundSlot(int slot) {
        return boundSlots.getOrDefault(slot, -1);
    }

    public static synchronized void bindSlots(int slotA, int slotB) {
        boundSlots.put(slotA, slotB);
        boundSlots.put(slotB, slotA);
        markConfigChanged();
    }

    public static synchronized void unbindSlots(int slotA, int slotB) {
        boundSlots.remove(slotA);
        boundSlots.remove(slotB);
        markConfigChanged();
    }

    // --- Equipment Skulls ---
    public static synchronized void setEqSkull(int slot, String textureUrl) {
        if (textureUrl == null) {
            equipmentSkullTextures.remove(slot);
        } else {
            equipmentSkullTextures.put(slot, textureUrl);
        }
        markConfigChanged();
    }

    public static synchronized String getEquipmentSkullTexture(int slot) {
        return equipmentSkullTextures.get(slot);
    }

    public static synchronized void setEqItemData(int slot, String itemData) {
        if (itemData == null) {
            equipmentSkullItemData.remove(slot);
        } else {
            equipmentSkullItemData.put(slot, itemData);
        }
        markConfigChanged();
    }

    public static synchronized String getEquipmentSkullItemData(int slot) {
        return equipmentSkullItemData.get(slot);
    }

    public static synchronized boolean hasEquipmentSkull(int slot) {
        return equipmentSkullTextures.containsKey(slot);
    }

    public static synchronized void clearEquipmentSkulls() {
        equipmentSkullTextures.clear();
        equipmentSkullItemData.clear();
        markConfigChanged();
    }

    public static synchronized Map<Integer, String> getAllEquipmentSkullTextures() {
        return new HashMap<>(equipmentSkullTextures);
    }

    public static synchronized void clearEquipmentSlot(int slot) {
        equipmentSkullTextures.remove(slot);
        equipmentSkullItemData.remove(slot);
        markConfigChanged();
    }

    // --- Config IO ---
    public static synchronized void load() {
        if (!CONFIG_FILE.exists()) {
            System.err.println("[ItemConfig] Config file does not exist. Creating a new one...");
            loadOrRestore();
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(CONFIG_FILE), java.nio.charset.StandardCharsets.UTF_8)) {
            Type configType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> config = GSON.fromJson(reader, configType);

            if (config == null || !validate(config)) {
                System.err.println("[ItemConfig] Invalid config detected. Attempting to restore from backup...");
                loadOrRestore();
                return;
            }

            // --- Load Item Protection ---
            if (config.containsKey(SELL_PROT_KEY)) {
                isSellProtectionEnabled = (Boolean) config.get(SELL_PROT_KEY);
            }
            if (config.containsKey(TOOLTIP_KEY)) {
                isTooltipEnabled = (Boolean) config.get(TOOLTIP_KEY);
            }
            if (config.containsKey(PROT_TRIGGER_KEY)) {
                isProtectTriggerEnabled = (Boolean) config.get(PROT_TRIGGER_KEY);
            }
            if (config.containsKey(PROT_NOTI_KEY)) {
                isProtectNotiEnabled = (Boolean) config.get(PROT_NOTI_KEY);
            }
            if (config.containsKey(UUIDS_KEY)) {
                Object uuidsObj = config.get(UUIDS_KEY);
                protectedUUIDs.clear();
                if (uuidsObj instanceof Map) {
                    Map<?, ?> uuidMap = (Map<?, ?>) uuidsObj;
                    for (Map.Entry<?, ?> entry : uuidMap.entrySet()) {
                        if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                            protectedUUIDs.put((String) entry.getKey(), (String) entry.getValue());
                        }
                    }
                } else if (uuidsObj instanceof List) {
                    // Backward compatibility
                    for (Object uuidObj : (List<?>) uuidsObj) {
                        if (uuidObj instanceof String string) {
                            protectedUUIDs.put(string, "");
                        }
                    }
                }
            }

            // --- Load Slot Locking ---
            if (config.containsKey(LOCK_TRIGGER_KEY)) {
                isLockTriggerEnabled = (Boolean) config.get(LOCK_TRIGGER_KEY);
            }
            if (config.containsKey(LOCKED_SLOTS_KEY)) {
                Object locked = config.get(LOCKED_SLOTS_KEY);
                if (locked instanceof List<?>) {
                    lockedSlots.clear();
                    for (Object o : (List<?>) locked) {
                        if (o instanceof Number number) lockedSlots.add(number.intValue());
                    }
                }
            }

            // --- Load Slot Binding ---
            if (config.containsKey(BOUND_SLOTS_KEY)) {
                Object bound = config.get(BOUND_SLOTS_KEY);
                if (bound instanceof Map<?, ?>) {
                    boundSlots.clear();
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) bound).entrySet()) {
                        int from = Integer.parseInt(e.getKey().toString());
                        int to = ((Number) e.getValue()).intValue();
                        boundSlots.put(from, to);
                        boundSlots.put(to, from);
                    }
                }
            }

            // --- Load Blacklist ---
            if (config.containsKey(BLACKLIST_KEY)) {
                Object blacklistObject = config.get(BLACKLIST_KEY);
                if (blacklistObject instanceof List) {
                    List<?> rawList = (List<?>) blacklistObject;
                    List<Map<String, Object>> entries = new ArrayList<>();
                    for (Object o : rawList) {
                        if (o instanceof Map<?, ?>) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> entry = (Map<String, Object>) o;
                            entries.add(entry);
                        }
                    }
                    BlacklistManager.loadUserBlacklistFromJson(entries);
                } else {
                    System.err.println("[ItemConfig] Blacklist is not a valid list.");
                }
            }

            // --- Load Equipment Skulls ---
            if (config.containsKey(EQUIPMENT_SKULL_TEXTURES_KEY)) {
                Object texturesObj = config.get(EQUIPMENT_SKULL_TEXTURES_KEY);
                if (texturesObj instanceof Map<?, ?>) {
                    equipmentSkullTextures.clear();
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) texturesObj).entrySet()) {
                        try {
                            int slot = Integer.parseInt(entry.getKey().toString());
                            String textureUrl = entry.getValue().toString();
                            equipmentSkullTextures.put(slot, textureUrl);
                        } catch (NumberFormatException e) {
                            System.err.println("[ItemConfig] Invalid equipment skull slot: " + entry.getKey());
                        }
                    }
                }
            }

            if (config.containsKey(EQUIPMENT_SKULL_ITEMS_KEY)) {
                Object itemsObj = config.get(EQUIPMENT_SKULL_ITEMS_KEY);
                if (itemsObj instanceof Map<?, ?>) {
                    equipmentSkullItemData.clear();
                    for (Map.Entry<?, ?> entry : ((Map<?, ?>) itemsObj).entrySet()) {
                        try {
                            int slot = Integer.parseInt(entry.getKey().toString());
                            String itemData = entry.getValue().toString();
                            equipmentSkullItemData.put(slot, itemData);
                        } catch (NumberFormatException e) {
                            System.err.println("[ItemConfig] Invalid equipment skull item slot: " + entry.getKey());
                        }
                    }
                }
            }
        } catch (JsonSyntaxException | com.google.gson.stream.MalformedJsonException e) {
            System.err.println("[ItemConfig] Malformed JSON detected: " + e.getMessage());
            loadOrRestore();
        } catch (IOException e) {
            System.err.println("[ItemConfig] Failed to load configuration: " + e.getMessage());
            loadOrRestore();
        }
    }

    public static synchronized void save() {
        Map<String, Object> config = new HashMap<>();
        // --- Save Item Protection ---
        config.put(UUIDS_KEY, new HashMap<>(protectedUUIDs));
        config.put(SELL_PROT_KEY, isSellProtectionEnabled);
        config.put(TOOLTIP_KEY, isTooltipEnabled);
        config.put(PROT_TRIGGER_KEY, isProtectTriggerEnabled);
        config.put(PROT_NOTI_KEY, isProtectNotiEnabled);

        // --- Save Slot Locking ---
        config.put(LOCK_TRIGGER_KEY, isLockTriggerEnabled);
        config.put(LOCKED_SLOTS_KEY, new ArrayList<>(lockedSlots));

        // --- Save Slot Binding ---
        Map<Integer, Integer> boundCopy = new HashMap<>();
        for (Map.Entry<Integer, Integer> e : boundSlots.entrySet()) {
            // Prevent saving mirrored pairs (e.g., both 10→17 and 17→10)
            if (e.getKey() < e.getValue()) {
                boundCopy.put(e.getKey(), e.getValue());
            }
        }
        config.put(BOUND_SLOTS_KEY, boundCopy);

        // --- Save Blacklist ---
        List<Map<String, Object>> serializedBlacklist = BlacklistManager.getUserBlacklistAsJson();
        config.put(BLACKLIST_KEY, serializedBlacklist);

        // --- Save Equipment Skulls ---
        config.put(EQUIPMENT_SKULL_TEXTURES_KEY, new HashMap<>(equipmentSkullTextures));
        config.put(EQUIPMENT_SKULL_ITEMS_KEY, new HashMap<>(equipmentSkullItemData));

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE), java.nio.charset.StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            System.err.println("[ItemConfig] Failed to save config: " + e.getMessage());
        }
    }

    public static void saveBackup() {
        try {
            if (CONFIG_FILE.exists()) {
                Files.copy(CONFIG_FILE.toPath(), BACKUP_FILE.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[ItemConfig] Backup saved successfully.");
            }
        } catch (IOException e) {
            System.err.println("[ItemConfig] Failed to save backup: " + e.getMessage());
        }
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
        // If restore fails or backup doesn't exist, create a new config
        System.err.println("[ItemConfig] Creating a new config file...");
        try {
            boolean created = CONFIG_FILE.createNewFile();
            if (created) {
                System.out.println("[ItemConfig] New config file created.");
            } else {
                System.out.println("[ItemConfig] Failed to create new config file.");
            }
            save();
            recreatedConfig = true;
        } catch (IOException e) {
            System.err.println("[ItemConfig] Failed to create new config file: " + e.getMessage());
        }
    }

    private static boolean validate(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return false;
        }
        // Check for required keys
        if (!config.containsKey(UUIDS_KEY) || !config.containsKey(SELL_PROT_KEY) ||
            !config.containsKey(TOOLTIP_KEY) || !config.containsKey(PROT_TRIGGER_KEY) ||
            !config.containsKey(PROT_NOTI_KEY) || !config.containsKey(LOCKED_SLOTS_KEY) ||
            !config.containsKey(LOCK_TRIGGER_KEY) || !config.containsKey(BOUND_SLOTS_KEY)){
            return false;
        }
        // Validate types
        boolean valid = (config.get(UUIDS_KEY) instanceof Map) &&
               (config.get(SELL_PROT_KEY) instanceof Boolean) &&
               (config.get(TOOLTIP_KEY) instanceof Boolean) &&
               (config.get(PROT_TRIGGER_KEY) instanceof Boolean) &&
               (config.get(PROT_NOTI_KEY) instanceof Boolean) &&
               (config.get(LOCK_TRIGGER_KEY) instanceof Boolean) &&
               (config.get(BOUND_SLOTS_KEY) instanceof Map) &&
               (config.get(LOCKED_SLOTS_KEY) instanceof List);
               
        // Optional
        if (config.containsKey(EQUIPMENT_SKULL_TEXTURES_KEY)) {
            valid &= (config.get(EQUIPMENT_SKULL_TEXTURES_KEY) instanceof Map);
        }
        if (config.containsKey(EQUIPMENT_SKULL_ITEMS_KEY)) {
            valid &= (config.get(EQUIPMENT_SKULL_ITEMS_KEY) instanceof Map);
        }
        
        return valid;
    }

    private static void markConfigChanged() {
        configChanged = true;
        saveConfigIfNeeded();
    }

    public static void saveConfigIfNeeded() {
        if (configChanged) {
            save();
            configChanged = false;
        }
    }
}
