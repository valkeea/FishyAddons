package me.valkeea.fishyaddons.vconfig.config.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import me.valkeea.fishyaddons.util.JsonUtil;
import me.valkeea.fishyaddons.vconfig.config.BaseConfig;
import me.valkeea.fishyaddons.vconfig.config.ConfigSection;
import net.minecraft.text.Text;

@SuppressWarnings("squid:S6548")
public class ItemConfig extends BaseConfig {
    private static final ItemConfig INSTANCE = new ItemConfig();
    
    private static final String LOCKED_PREFIX = "locked_";
    private static final String BOUND_PREFIX = "bound_";
    private static final String ITEMSTACK_PREFIX = "itemstack_";
    private static final String EQ_STACKS = "equipmentItemStacks";
    private static final String GUI_ICONS_SCREENS = "guiicons_screenNames";
    private static final String UUIDS_KEY = "protectedUUIDs";
    public static final String LOCKED_KEY = "lockedSlots";
    public static final String BOUND_KEY = "boundSlots";    
    
    private final ConfigSection<String> protectedItems =
        new ConfigSection<>(UUIDS_KEY,
            new TypeToken<Map<String, String>>(){}.getType(),
            v -> requestSave());
    
    private final ConfigSection<Integer> slotData =
        new ConfigSection<>("_slotData_internal",
            new TypeToken<Map<String, Integer>>(){}.getType(),
            v -> requestSave());
    
    private final ConfigSection<String> equipmentData =
        new ConfigSection<>("_equipmentData_internal",
            new TypeToken<Map<String, String>>(){}.getType(),
            v -> requestSave());
    
    private final ConfigSection<Boolean> guiIconsScreenNames =
        new ConfigSection<>(GUI_ICONS_SCREENS,
            new TypeToken<Map<String, Boolean>>(){}.getType(),
            v -> requestSave());
    
    private final ConfigSection<Set<Integer>> guiIconsScreenSlotMap =
        new ConfigSection<>("guiicons_screenSlotMap",
            new TypeToken<Map<String, Set<Integer>>>(){}.getType(),
            v -> requestSave());
    
    private ItemConfig() {
        super("fishyitems.json");
    }
    
    public static ItemConfig getInstance() {
        return INSTANCE;
    }
    
    @Override
    protected void loadFromJson(JsonObject json) {
        if (json.has(UUIDS_KEY)) {
            JsonElement element = json.get(UUIDS_KEY);
            if (element.isJsonObject()) {
                protectedItems.loadFromJson(json);
            } else if (element.isJsonArray()) {
                // Legacy array format - convert to map
                JsonArray array = element.getAsJsonArray();
                for (JsonElement item : array) {
                    if (item.isJsonPrimitive()) {
                        String uuid = item.getAsString();
                        protectedItems.set(uuid, "");
                    }
                }
            }
        }
        
        loadSlotData(json);
        loadEquipmentData(json);
        loadGuiIconsConfig(json);
    }
    
    @Override
    protected void saveToJson(JsonObject json) {

        protectedItems.saveToJson(json);
        saveSlotData(json);
        saveEquipmentData(json);
        saveGuiIconsConfig(json);
    }
    
    private void loadSlotData(JsonObject json) {
        // Locked slots (stored as array [1, 2, 3])
        if (json.has(LOCKED_KEY)) {
            JsonElement element = json.get(LOCKED_KEY);
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
        
        // Bound slots (stored as object {"1": 2, "3": 4})
        if (json.has(BOUND_KEY)) {
            JsonElement element = json.get(BOUND_KEY);
            if (element.isJsonObject()) {
                JsonObject boundObj = element.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : boundObj.entrySet()) {
                    try {
                        int from = Integer.parseInt(entry.getKey());
                        int to = entry.getValue().getAsInt();
                        slotData.set(BOUND_PREFIX + from, to);
                        slotData.set(BOUND_PREFIX + to, from);
                    } catch (NumberFormatException e) {
                        logError("Invalid bound slot key: " + entry.getKey());
                    }
                }
            }
        }
    }
    
    private void saveSlotData(JsonObject json) {
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
                    logError("Invalid bound slot key: " + key);
                }
            }
        }
        
        json.add(LOCKED_KEY, GSON.toJsonTree(lockedSlots));
        
        // Save bound slots as filtered map
        Map<Integer, Integer> filteredBoundSlots = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : boundSlots.entrySet()) {
            int from = entry.getKey();
            int to = entry.getValue();
            if (from < to) {
                filteredBoundSlots.put(from, to);
            }
        }
        json.add(BOUND_KEY, GSON.toJsonTree(filteredBoundSlots));
    }
    
    private void loadEquipmentData(JsonObject json) {

        if (json.has(EQ_STACKS)) {
            JsonElement element = json.get(EQ_STACKS);

            if (element.isJsonObject()) {
                JsonObject itemStacksObj = element.getAsJsonObject();

                for (var entry : itemStacksObj.entrySet()) {
                    try {
                        int slot = Integer.parseInt(entry.getKey());
                        String itemStackData = entry.getValue().getAsString();
                        equipmentData.set(ITEMSTACK_PREFIX + slot, itemStackData);

                    } catch (NumberFormatException e) {
                        logError("Invalid equipment slot: " + entry.getKey());
                    }
                }
            }
        }
    }
    
    private void saveEquipmentData(JsonObject json) {
        Map<String, String> itemStacks = new HashMap<>();
        
        for (var entry : equipmentData.getValues().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (key.startsWith(ITEMSTACK_PREFIX)) {
                String slotStr = key.substring(ITEMSTACK_PREFIX.length());
                itemStacks.put(slotStr, value);
            }
        }
        
        if (!itemStacks.isEmpty()) {
            json.add(EQ_STACKS, GSON.toJsonTree(itemStacks));
        }
    }
    
    private void loadGuiIconsConfig(JsonObject json) {
        if (json.has(GUI_ICONS_SCREENS)) {
            JsonElement element = json.get(GUI_ICONS_SCREENS);
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                for (JsonElement item : array) {
                    if (item.isJsonPrimitive()) {
                        String screenName = item.getAsString();
                        guiIconsScreenNames.set(screenName, true);
                    }
                }
            }
        }
        
        if (json.has("guiicons_screenSlotMap")) {
            guiIconsScreenSlotMap.loadFromJson(json);
        }
    }
    
    private void saveGuiIconsConfig(JsonObject json) {
        if (!guiIconsScreenNames.getValues().isEmpty()) {
            List<String> screenNamesList = new ArrayList<>(guiIconsScreenNames.getValues().keySet());
            json.add(GUI_ICONS_SCREENS, GSON.toJsonTree(screenNamesList));
        }
        
        // Save screen slot map directly
        if (!guiIconsScreenSlotMap.getValues().isEmpty()) {
            guiIconsScreenSlotMap.saveToJson(json);
        }
    }
    
    // --- Protected Items ---
    
    public static synchronized void addUUID(String uuid, Text displayName) {
        String serialized = JsonUtil.serializeText(displayName);
        INSTANCE.protectedItems.set(uuid, serialized);
    }
    
    public static synchronized void removeUUID(String uuid) {
        INSTANCE.protectedItems.remove(uuid);
    }
    
    public static synchronized void clearAll() {
        INSTANCE.protectedItems.clear();
    }
    
    public static synchronized boolean isProtected(String uuid) {
        return INSTANCE.protectedItems.getValues().containsKey(uuid);
    }
    
    public static synchronized Text getDisplayName(String uuid) {
        String serialized = INSTANCE.protectedItems.getValues().get(uuid);
        return serialized != null ? JsonUtil.deserializeText(serialized) : null;
    }
    
    public static synchronized Map<String, String> getProtectedUUIDs() {
        return new HashMap<>(INSTANCE.protectedItems.getValues());
    }
    
    // --- Slot Locking ---
    
    public static synchronized boolean isSlotLocked(int slot) {
        return INSTANCE.slotData.getValues().containsKey(LOCKED_PREFIX + slot);
    }
    
    public static synchronized void toggleSlotLock(int slot) {
        String key = LOCKED_PREFIX + slot;
        if (INSTANCE.slotData.getValues().containsKey(key)) {
            INSTANCE.slotData.remove(key);
        } else {
            INSTANCE.slotData.set(key, slot);
        }
    }
    
    // --- Slot Binding ---
    
    public static synchronized boolean areSlotsBound(int slotA, int slotB) {
        Integer boundSlotA = INSTANCE.slotData.getValues().get(BOUND_PREFIX + slotA);
        return boundSlotA != null && boundSlotA == slotB;
    }
    
    public static synchronized boolean isSlotBound(int slot) {
        return INSTANCE.slotData.getValues().containsKey(BOUND_PREFIX + slot);
    }
    
    public static synchronized int getBoundSlot(int slot) {
        Integer bound = INSTANCE.slotData.getValues().get(BOUND_PREFIX + slot);
        return bound != null ? bound : -1;
    }
    
    public static synchronized void bindSlots(int slotA, int slotB) {
        INSTANCE.slotData.set(BOUND_PREFIX + slotA, slotB);
        INSTANCE.slotData.set(BOUND_PREFIX + slotB, slotA);
    }
    
    public static synchronized void unbindSlots(int slotA, int slotB) {
        INSTANCE.slotData.remove(BOUND_PREFIX + slotA);
        INSTANCE.slotData.remove(BOUND_PREFIX + slotB);
    }
    
    // --- Equipment ItemStacks ---
    
    public static synchronized void clearEquipmentSlot(int slot) {
        INSTANCE.equipmentData.remove(ITEMSTACK_PREFIX + slot);
    }
    
    public static synchronized void setEqItemStack(int slot, String serializedItemStack) {
        String key = ITEMSTACK_PREFIX + slot;
        if (serializedItemStack == null || serializedItemStack.isEmpty()) {
            INSTANCE.equipmentData.remove(key);
        } else {
            INSTANCE.equipmentData.set(key, serializedItemStack);
        }
    }
    
    public static synchronized String getEqItemStack(int slot) {
        return INSTANCE.equipmentData.getValues().get(ITEMSTACK_PREFIX + slot);
    }
    
    public static synchronized Map<Integer, String> getAllEqItemStacks() {
        Map<Integer, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : INSTANCE.equipmentData.getValues().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(ITEMSTACK_PREFIX)) {
                try {
                    int slot = Integer.parseInt(key.substring(ITEMSTACK_PREFIX.length()));
                    result.put(slot, entry.getValue());
                } catch (NumberFormatException e) {
                    INSTANCE.logError("Invalid itemstack slot key: " + key);
                }
            }
        }
        return result;
    }
    
    // --- GuiIcons ---
    
    public static synchronized Set<String> getGuiIconsScreenNames() {
        return new HashSet<>(INSTANCE.guiIconsScreenNames.getValues().keySet());
    }
    
    public static synchronized Map<String, Set<Integer>> getGuiIconsScreenSlotMap() {
        Map<String, Set<Integer>> copy = new HashMap<>();
        for (Map.Entry<String, Set<Integer>> e : INSTANCE.guiIconsScreenSlotMap.getValues().entrySet()) {
            copy.put(e.getKey(), new HashSet<>(e.getValue()));
        }
        return copy;
    }
    
    public static synchronized void addGuiIconsScreen(String screenName) {
        if (screenName != null && !screenName.isEmpty()) {
            INSTANCE.guiIconsScreenNames.set(screenName, true);
        }
    }
    
    public static synchronized void removeGuiIconsScreen(String screenName) {
        if (screenName != null) {
            INSTANCE.guiIconsScreenNames.getValues().keySet()
                .removeIf(name -> name.equalsIgnoreCase(screenName));
            
            String key = screenName.toLowerCase(java.util.Locale.ROOT);
            INSTANCE.guiIconsScreenSlotMap.remove(key);
        }
    }
    
    public static synchronized void addGuiIconsSlot(String screenName, int slotId) {
        if (screenName != null && !screenName.isEmpty()) {
            String key = screenName.toLowerCase(java.util.Locale.ROOT);
            Set<Integer> slots = INSTANCE.guiIconsScreenSlotMap.get(key);
            if (slots == null) {
                slots = new HashSet<>();
                INSTANCE.guiIconsScreenSlotMap.set(key, slots);
            }
            slots.add(slotId);
        }
    }
    
    public static synchronized void removeGuiIconsSlot(String screenName, int slotId) {
        if (screenName != null) {
            String key = screenName.toLowerCase(java.util.Locale.ROOT);
            Set<Integer> slots = INSTANCE.guiIconsScreenSlotMap.get(key);
            if (slots != null) {
                slots.remove(slotId);
                if (slots.isEmpty()) {
                    INSTANCE.guiIconsScreenSlotMap.remove(key);
                }
            }
        }
    }
    
    public static void saveGuiIcons() {
        INSTANCE.requestSave();
    }
}
