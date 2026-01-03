package me.valkeea.fishyaddons.feature.item.safeguard;

import java.util.Map;
import java.util.Set;

import me.valkeea.fishyaddons.config.ItemConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtString;

public class ItemHandler {
    private ItemHandler() {}

    public static boolean isProtected(ItemStack stack) {
        String uuid = extractUUID(stack);
        return !uuid.isEmpty() && ItemConfig.isProtected(uuid);
    }

    public static String extractUUID(ItemStack stack) {
        var component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component == null)  return "";
        var uuidElement = component.copyNbt().get("uuid");
        return uuidElement instanceof NbtString uuid ? uuid.asString().orElse("") : "";
    }

    public static Set<String> getProtectedUUIDs() {
        return ItemConfig.getProtectedUUIDs().keySet();
    }

    public static Map<String, String> getProtectedUUIDsWithNames() {
        return ItemConfig.getProtectedUUIDs();
    }
}
