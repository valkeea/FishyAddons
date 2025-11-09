package me.valkeea.fishyaddons.feature.item.safeguard;

import java.util.Map;
import java.util.WeakHashMap;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.ScreenClickEvent;
import me.valkeea.fishyaddons.tool.PlaySound;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public class GuiHandler {
    private GuiHandler() {}
    private static final Map<ItemStack, Boolean> protectionCache = new WeakHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;

    public static void init() {
        FaEvents.SCREEN_MOUSE_CLICK.register(event -> {
            if (handleClick(event)) {
                event.setConsumed(true);
            }
        }, EventPriority.HIGH, EventPhase.PRE);
    }

    private static boolean handleClick(ScreenClickEvent event) {

        var mc = MinecraftClient.getInstance();
        ItemStack stack;

        if (event.hoveredSlot != null) {
            stack = event.hoveredSlot.getStack();
        } else {
            stack = mc.player.currentScreenHandler.getCursorStack();
        }

        if (stack == null || stack.isEmpty()) return false;
        if (!isProtectedCached(stack)) return false;

        if (BlacklistMatcher.isBlacklistedGUI(event.screen, event.screen.getClass().getName()) && 
            ItemConfig.isSellProtectionEnabled()) {
            triggerProtection();
            event.setConsumed(true);

            return true;
        }
        return false;
    }    

    public static boolean isProtectedCached(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        if (protectionCache.size() > MAX_CACHE_SIZE) {
            protectionCache.clear();
        }

        return protectionCache.computeIfAbsent(stack, s -> {
            try {
                String uuid = ItemHandler.extractUUID(s);
                if (uuid.isEmpty()) return false;
                
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
