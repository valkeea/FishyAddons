package me.valkeea.fishyaddons.feature.skyblock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.valkeea.fishyaddons.util.JsonUtil;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.config.impl.ItemConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ResolvableProfile;

public class EqTextures {
    private EqTextures() {}
    private static final Logger LOGGER = LoggerFactory.getLogger(EqTextures.class);
    
    private static final Map<Integer, ItemStack> eqItemStacks = new HashMap<>();
    private static final Map<Integer, List<Component>> eqTooltips = new HashMap<>();
    private static final Map<Integer, ResolvableProfile> skullProfiles = new HashMap<>();    
    private static final Map<Integer, Boolean> emptySlots = new HashMap<>();
    
    private static boolean dataLoaded = false;

    private static void ensureDataLoaded() {
        if (!dataLoaded) {
            loadSkullData();
            dataLoaded = true;
        }
    }

    public static void saveEqTexture(ItemStack itemStack, int slotIndex, boolean isSkull) {
        if (!Config.get(BooleanKey.EQ_DISPLAY)) return;
        
        if (isSkull) {
            var profile = itemStack.get(DataComponents.PROFILE);
            if (profile != null) {
                skullProfiles.put(slotIndex, profile);
            }
        }

        eqItemStacks.put(slotIndex, itemStack.copy());
        emptySlots.put(slotIndex, false);
        eqTooltips.put(slotIndex, buildTooltip(itemStack));
        
        String serialized = JsonUtil.serializeItemStack(itemStack);
        if (serialized != null && !serialized.isEmpty()) {
            ItemConfig.setEqItemStack(slotIndex, serialized);
        }
    }

    private static List<Component> buildTooltip(ItemStack itemStack) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return List.of();

        var flag = mc.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
        return itemStack.getTooltipLines(TooltipContext.of(mc.level), mc.player, flag);
    }

    /**
     * Mark the given equipment slot as empty
     */
    public static void saveEmptySlot(int slotIndex) {
        if (!Config.get(BooleanKey.EQ_DISPLAY)) return;
        
        eqItemStacks.remove(slotIndex);
        skullProfiles.remove(slotIndex);
        eqTooltips.remove(slotIndex);
        emptySlots.put(slotIndex, true);
        
        ItemConfig.clearEquipmentSlot(slotIndex);
    }    

    /** Check if the specified equipment slot has any saved data */
    public static boolean hasSlotData(int slotIndex) {
        ensureDataLoaded();
        return emptySlots.containsKey(slotIndex) || eqItemStacks.containsKey(slotIndex);
    }

    /** Check if any equipment slot currently has saved data */
    public static boolean hasAnyData() {
        ensureDataLoaded();
        for (int i = 0; i < 4; i++) {
            if (hasSlotData(i)) return true;
        }
        return false;
    }

    /** Check if the specified equipment slot is empty */
    public static boolean isEmptySlot(int slotIndex) {
        ensureDataLoaded();
        return !eqItemStacks.containsKey(slotIndex) && 
               emptySlots.getOrDefault(slotIndex, false);
    }

    /** Get the saved ItemStack for the specified equipment slot */
    public static ItemStack getSlotItemStack(int slotIndex) {
        ensureDataLoaded();
        if (!eqItemStacks.containsKey(slotIndex)) {
            loadSlotFromConfig(slotIndex);
        }
        return eqItemStacks.getOrDefault(slotIndex, null);
    }
    
    /** Get the saved tooltip lines for the specified equipment slot */
    public static List<Component> getSlotTooltip(int slotIndex) {
        if (!Config.get(BooleanKey.EQ_DISPLAY_TOOLTIP)) return List.of();
        ensureDataLoaded();

        if (!eqTooltips.containsKey(slotIndex)) {
            loadSlotFromConfig(slotIndex);
        }
        return eqTooltips.getOrDefault(slotIndex, List.of());
    }
    
    public static void clearAll() {
        eqItemStacks.clear();
        skullProfiles.clear();
        eqTooltips.clear();
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

        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var itemStack = JsonUtil.deserializeItemStack(data);
        if (!isEqItem(itemStack))  return;
        
        eqItemStacks.put(slot, itemStack);
        eqTooltips.put(slot, JsonUtil.extractTooltip(data));
        
        var profile = itemStack.get(DataComponents.PROFILE);
        if (profile != null) {
            skullProfiles.put(slot, profile);
        }
    }

    private static void loadSlotFromConfig(int slotIndex) {

        String serialized = ItemConfig.getEqItemStack(slotIndex);
        if (serialized != null && !serialized.isEmpty()) {

            var itemStack = JsonUtil.deserializeItemStack(serialized);
            if (isEqItem(itemStack)) {
                eqItemStacks.put(slotIndex, itemStack);
                eqTooltips.put(slotIndex, JsonUtil.extractTooltip(serialized));
            }
        }
    }

    private static boolean isEqItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var item = stack.getItem();
        return item == Items.PLAYER_HEAD || item == Items.PAPER;
    }
}
