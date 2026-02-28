package me.valkeea.fishyaddons.feature.item.safeguard;

import java.util.Map;
import java.util.Set;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.tool.ItemData;
import net.minecraft.item.ItemStack;

public class ItemHandler {
    private ItemHandler() {}

    public static boolean isProtected(ItemStack stack) {
        String uuid = ItemData.extractUUID(stack);
        return !uuid.isEmpty() && ItemConfig.isProtected(uuid);
    }

    public static Set<String> getProtectedUUIDs() {
        return ItemConfig.getProtectedUUIDs().keySet();
    }

    public static Map<String, String> getProtectedUUIDsWithNames() {
        return ItemConfig.getProtectedUUIDs();
    }
}
