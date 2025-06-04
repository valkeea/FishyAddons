package me.wait.fishyaddons.handlers;

import java.util.Map;
import java.util.Set;

import me.wait.fishyaddons.config.UUIDConfigHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ProtectedItemHandler {

    private ProtectedItemHandler() {
        throw new UnsupportedOperationException("wee");
    }

    public static boolean isProtected(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return false;

        // Short-circuit early
        if (!stack.getTagCompound().hasKey("ExtraAttributes")) return false;

        NBTTagCompound extra = stack.getSubCompound("ExtraAttributes", false);
        if (extra == null || !extra.hasKey("uuid")) return false;

        return UUIDConfigHandler.isProtected(extra.getString("uuid"));
    }

    public static Set<String> getProtectedUUIDs() {
        return UUIDConfigHandler.getProtectedUUIDs().keySet();
    }

    public static Map<String, String> getProtectedUUIDsWithNames() {
        return UUIDConfigHandler.getProtectedUUIDs();
    }
}
