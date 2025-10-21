package me.valkeea.fishyaddons.safeguard;

import java.util.Map;
import java.util.WeakHashMap;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;

public class SellProtectionHandler {
    private SellProtectionHandler() {}
    private static final Map<ItemStack, Boolean> protectionCache = new WeakHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    public static boolean isProtectedCached(ItemStack stack, RegistryWrapper.WrapperLookup registries) {
        if (stack == null || stack.isEmpty() || registries == null) return false;

        if (protectionCache.size() > MAX_CACHE_SIZE) {
            protectionCache.clear();
        }

        return protectionCache.computeIfAbsent(stack, s -> {
            try {
                String uuid = ItemHandler.extractUUID(s, registries);
                if (uuid == null) {
                    return false;
                }
                return ItemConfig.isProtected(uuid);
            } catch (RuntimeException e) {
                return false;
            }
        });
    }

    public static void triggerProtection() {
        if (ItemConfig.isProtectTriggerEnabled()) {
            PlaySound.protectTrigger();
        }
        if (ItemConfig.isProtectNotiEnabled()) {
            FishyNotis.protectNoti();
        }
    }
}
