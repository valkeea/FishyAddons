package me.valkeea.fishyaddons.safeguard;

import java.util.Map;
import java.util.Set;

import me.valkeea.fishyaddons.config.ItemConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;

public class ItemHandler {
    private ItemHandler() {}

    public static boolean isProtected(ItemStack stack) {
        String uuid = extractUUID(stack);
        return uuid != null && ItemConfig.isProtected(uuid);
    }

    public static String extractUUID(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, null);
        NbtElement uuidElement = component.copyNbt().get("uuid");
        return uuidElement instanceof NbtString uuid ? uuid.asString().orElse("") : "test";
    }

    public static Set<String> getProtectedUUIDs() {
        return ItemConfig.getProtectedUUIDs().keySet();
    }

    public static Map<String, String> getProtectedUUIDsWithNames() {
        return ItemConfig.getProtectedUUIDs();
    }
}