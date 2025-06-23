package me.valkeea.fishyaddons.safeguard;

import me.valkeea.fishyaddons.config.ItemConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;

import java.util.Map;
import java.util.Set;

public class ItemHandler {
    private ItemHandler() {}

    public static boolean isProtected(ItemStack stack, RegistryWrapper.WrapperLookup registries) {
        String uuid = extractUUID(stack, registries);
        return uuid != null && ItemConfig.isProtected(uuid);
    }

    // ---- Extract the UUID from an ItemStack's NBT data. ----
    public static String extractUUID(ItemStack stack, RegistryWrapper.WrapperLookup registries) {
        if (stack == null || stack.isEmpty() || registries == null) return null;

        NbtElement encoded = stack.toNbt(registries);
        if (!(encoded instanceof NbtCompound full)) return null;

        // Modern layout just in case we get funny updates (1.19+)
        NbtElement tagElement = full.get("tag");
        if (tagElement instanceof NbtCompound tag) {
            NbtElement extraElement = tag.get("ExtraAttributes");
            if (extraElement instanceof NbtCompound extra) {
                NbtElement uuidElement = extra.get("uuid");
                if (uuidElement instanceof net.minecraft.nbt.NbtString uuidStr) {
                    return uuidStr.asString().orElse("");
                }
            }
        }

        // Hypixel 1.8.9 layout
        NbtElement componentsElement = full.get("components");
        if (componentsElement instanceof NbtCompound components) {
            // Try direct "uuid"
            NbtElement uuidElement = components.get("uuid");
            if (uuidElement instanceof NbtString uuidStr) {
                return uuidStr.asString().orElse("");
            }
            // Try "minecraft:custom_data" -> "uuid"
            NbtElement customDataElement = components.get("minecraft:custom_data");
            if (customDataElement instanceof NbtCompound customData) {
                NbtElement uuidElement2 = customData.get("uuid");
                if (uuidElement2 instanceof NbtString uuidStr2) {
                    return uuidStr2.asString().orElse("");
                }
            }
        }
        return null;
    }

    public static Set<String> getProtectedUUIDs() {
        return ItemConfig.getProtectedUUIDs().keySet();
    }

    public static Map<String, String> getProtectedUUIDsWithNames() {
        return ItemConfig.getProtectedUUIDs();
    }
}


