package me.valkeea.fishyaddons.feature.skyblock;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.util.JsonUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class EqTextures {
    private EqTextures() {}
    private static final Logger LOGGER = LoggerFactory.getLogger("FishyAddons/SkullTexture");
    
    private static final Map<Integer, ItemStack> skullItemStacks = new HashMap<>();
    private static final Map<Integer, ProfileComponent> skullProfiles = new HashMap<>();
    private static final Map<Integer, Boolean> emptySlots = new HashMap<>();
    
    private static boolean dataLoaded = false;

    private static void ensureDataLoaded() {
        if (!dataLoaded) {
            loadSkullData();
            dataLoaded = true;
        }
    }
    
    /**
     * Save the skull texture and profile for the given equipment slot
     */
    public static void saveSkullTexture(int slotIndex, ItemStack itemStack) {
        if (!FishyConfig.getState(Key.EQ_DISPLAY, false)) return;
        
        var profile = itemStack.getOrDefault(DataComponentTypes.PROFILE, null);
        if (profile != null) {
            skullProfiles.put(slotIndex, profile);
        }

        skullItemStacks.put(slotIndex, itemStack.copy());
        emptySlots.put(slotIndex, false);
        
        String serialized = JsonUtil.serializeItemStack(itemStack);
        if (serialized != null && !serialized.isEmpty()) {
            ItemConfig.setEqItemStack(slotIndex, serialized);
        }
        invalidateDisplayCache();
    }

    /**
     * Mark the given equipment slot as empty
     */
    public static void saveEmptySlot(int slotIndex) {
        if (!FishyConfig.getState(Key.EQ_DISPLAY, false)) return;
        
        skullItemStacks.remove(slotIndex);
        skullProfiles.remove(slotIndex);
        emptySlots.put(slotIndex, true);
        
        ItemConfig.clearEquipmentSlot(slotIndex);
        invalidateDisplayCache();
    }    
    
    /**
     * Notify display that data has changed and cache should be refreshed
     */
    private static void invalidateDisplayCache() {
        try {
            Class<?> eqDisplayClass = Class.forName("me.valkeea.fishyaddons.hud.ui.EqDisplay");
            var method = eqDisplayClass.getMethod("requestRefresh");
            method.invoke(null);
        } catch (Exception e) {
            LOGGER.debug("Could not notify EqDisplay of data change: {}", e.getMessage());
        }
    }

    /** Check if the specified equipment slot has any saved data */
    public static boolean hasSlotData(int slotIndex) {
        ensureDataLoaded();
        return emptySlots.containsKey(slotIndex) || skullItemStacks.containsKey(slotIndex);
    }

    /** Check if the specified equipment slot is empty */
    public static boolean isEmptySlot(int slotIndex) {
        ensureDataLoaded();
        return !skullItemStacks.containsKey(slotIndex) && 
               emptySlots.getOrDefault(slotIndex, false);
    }

    /** Get the saved ItemStack for the specified equipment slot */
    public static ItemStack getSlotItemStack(int slotIndex) {
        ensureDataLoaded();
        if (!skullItemStacks.containsKey(slotIndex)) {
            loadSlotFromConfig(slotIndex);
        }
        return skullItemStacks.getOrDefault(slotIndex, null);
    }
    
    public static void clearAll() {
        skullItemStacks.clear();
        skullProfiles.clear();
        emptySlots.clear();

        Map<Integer, String> itemStacks = ItemConfig.getAllEqItemStacks();

        for (Integer slot : itemStacks.keySet()) {
            ItemConfig.clearEquipmentSlot(slot);
        }
    }

    private static void loadSkullData() {
        try {
            load();
        } catch (Exception e) {
            LOGGER.error("Failed to load skull data from ItemConfig", e);
        }
    }
    
    private static void load() {
        Map<Integer, String> serialized = ItemConfig.getAllEqItemStacks();

        for (Map.Entry<Integer, String> entry : serialized.entrySet()) {
            int slot = entry.getKey();
            String data = entry.getValue();
            
            if (data != null && !data.isEmpty()) {
                loadItemStack(slot, data);
            }
        }
    }

    private static void loadItemStack(int slot, String data) {

        var client = MinecraftClient.getInstance();
        if (client.world == null) return;

        var itemStack = JsonUtil.deserializeItemStack(data);
        if (itemStack.isEmpty() || itemStack.getItem() != Items.PLAYER_HEAD) {
            LOGGER.debug("Failed to deserialize or invalid ItemStack for slot {}", slot);
            return;
        }
        
        skullItemStacks.put(slot, itemStack);
        
        var profile = itemStack.getOrDefault(DataComponentTypes.PROFILE, null);
        if (profile != null) {
            skullProfiles.put(slot, profile);
        }
    }

    private static void loadSlotFromConfig(int slotIndex) {
        String serialized = ItemConfig.getEqItemStack(slotIndex);
        if (serialized != null && !serialized.isEmpty()) {
            var itemStack = JsonUtil.deserializeItemStack(serialized);
            if (!itemStack.isEmpty() && itemStack.getItem() == Items.PLAYER_HEAD) {
                skullItemStacks.put(slotIndex, itemStack);
            }
        }
    }    
}
