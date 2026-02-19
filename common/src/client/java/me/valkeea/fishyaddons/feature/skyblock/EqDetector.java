package me.valkeea.fishyaddons.feature.skyblock;

import java.util.HashMap;
import java.util.Map;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * Detects init (changes) in equipment screen and updates EqTextures accordingly
 */
public class EqDetector {
    
    private static boolean isEqScreen = false;
    private static GenericContainerScreen currentEqScreen = null;
    private static final int[] EQUIPMENT_SLOTS = {10, 19, 28, 37};
    private static final Map<Integer, ItemStack> lastSeenStacks = new HashMap<>();

    private EqDetector() {}

    public static void init() {
        FaEvents.SCREEN_OPEN.register(event -> onScreen(event.screen, event.titleString));
        FaEvents.SCREEN_CLOSE.register(event -> onScreenClosed(event.titleString));
    }

    public static void onScreen(GenericContainerScreen screen, String title) {
        if (!FishyConfig.getState(Key.EQ_DISPLAY, false) || !isEqScreen(title)) return;
        
        isEqScreen = true;
        currentEqScreen = screen;

        String taskName = "eq_scan_" + System.currentTimeMillis();
        me.valkeea.fishyaddons.tool.RunDelayed.run(() -> {
            if (isEqScreen && currentEqScreen == screen) {
                scanEquipmentSlots(screen, true);
            }
        }, 200L, taskName);
    } 
    
    public static boolean isEqScreen(String title) {
        return title.contains("Your Equipment and Stats");
    }

    public static void onScreenClosed(String title) {
        if (isEqScreen(title)) {
            isEqScreen = false;
            currentEqScreen = null;
            lastSeenStacks.clear();
        }
    }
    
    public static void triggerRescan() {
        if (isEqScreen && currentEqScreen != null) {
            scanEquipmentSlots(currentEqScreen, true);
        }
    }
    
    private static void scanEquipmentSlots(GenericContainerScreen screen, boolean forceUpdate) {
        if (!FishyConfig.getState(Key.EQ_DISPLAY, false)) return;

        var handler = screen.getScreenHandler();
        if (handler == null) return;
        
        for (int i = 0; i < EQUIPMENT_SLOTS.length; i++) {
            int slotIndex = EQUIPMENT_SLOTS[i];

            if (slotIndex >= 0 && slotIndex < handler.slots.size()) {
                Slot slot = handler.slots.get(slotIndex);
                var currentStack = slot.getStack();
                var lastStack = lastSeenStacks.get(i);
                
    
                if (forceUpdate || !ItemStack.areEqual(currentStack, lastStack)) {
                    update(currentStack, i);
                }
            }
        }
    }

    private static void update(ItemStack currentStack, int i) {
        if (currentStack == null || currentStack.isEmpty()) {
            lastSeenStacks.remove(i);
            EqTextures.saveEmptySlot(i);
        } else {
            lastSeenStacks.put(i, currentStack.copy());

            if (currentStack.getItem().toString().contains("player_head")) {
                EqTextures.saveSkullTexture(i, currentStack);
            } else {
                EqTextures.saveEmptySlot(i);
            }
        }
    }
}
