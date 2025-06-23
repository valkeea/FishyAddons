package me.valkeea.fishyaddons.safeguard;

import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.PlaySound;
import me.valkeea.fishyaddons.config.ItemConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;

import java.util.Map;
import java.util.WeakHashMap;

public class SellProtectionHandler {
    private SellProtectionHandler() {}
    private static final Map<ItemStack, Boolean> protectionCache = new WeakHashMap<>();

    public static boolean isProtectedCached(ItemStack stack, RegistryWrapper.WrapperLookup registries) {
        if (stack == null || stack.isEmpty() || registries == null) return false;

        // Compute and cache protection status if not already cached
        return protectionCache.computeIfAbsent(stack, s -> {
            String uuid = ItemHandler.extractUUID(s, registries);
            if (uuid == null) {
                return false;
            }
            return ItemConfig.isProtected(uuid);
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
