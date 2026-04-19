package me.valkeea.fishyaddons.feature.item.safeguard;

import java.util.HashMap;
import java.util.Map;

import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.ScreenClickEvent;
import me.valkeea.fishyaddons.tool.ItemData;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.config.impl.ItemConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;

public class GuiHandler {
    private GuiHandler() {}
    private static final Map<String, Boolean> protectionCache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 200;

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
        int slotId = -1;

        if (event.hoveredSlot != null) {
            stack = event.hoveredSlot.getStack();
            slotId = event.hoveredSlot.id;
        } else {
            stack = mc.player.currentScreenHandler.getCursorStack();
        }

        if (stack == null || stack.isEmpty() || !isProtectedCached(stack)) return false;
        if (!(event.screen instanceof GenericContainerScreen gcs)) return false;
        
        if (BlacklistMatcher.isBlacklistedGUI(gcs) && Config.get(BooleanKey.FG_GUI_PROTECTION)) {
            int remapped = SlotHandler.remap(event.screen, slotId);
            if (remapped != -1) {
                FGUtil.triggerProtection();
                return true;
            }
        }
        
        return false;
    }    

    public static boolean isProtectedCached(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        String uuid = ItemData.extractUUID(stack);
        if (uuid.isEmpty()) return false;

        if (protectionCache.size() > MAX_CACHE_SIZE) {
            protectionCache.clear();
        }

        return protectionCache.computeIfAbsent(
            uuid, ItemConfig::isProtected
        );
    }
    
    public static void clearCache() {
        protectionCache.clear();
    }
}
