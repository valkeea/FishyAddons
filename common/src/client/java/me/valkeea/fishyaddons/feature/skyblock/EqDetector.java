package me.valkeea.fishyaddons.feature.skyblock;

import java.util.HashMap;
import java.util.Map;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import me.valkeea.fishyaddons.tool.RunDelayed;

/**
 * Detects init (changes) in equipment screen and updates EqTextures accordingly
 */
public class EqDetector {
    
    private static boolean isEqScreen = false;
    private static ContainerScreen currentEqScreen = null;
    private static final int WARDROBE_COLUMNS = 9;
    private static final int ARMOR_ROWS = 4;
    private static final int STATUS_ROW = 4;
    private static final Map<Integer, ItemStack> lastSeenStacks = new HashMap<>();

    private EqDetector() {}

    public static void init() {
        FaEvents.SCREEN_OPEN.register(event -> onScreen(event.screen, event.titleString));
        FaEvents.SCREEN_CLOSE.register(event -> onScreenClosed(event.titleString));
    }

    public static void onScreen(ContainerScreen screen, String title) {
        if (!Config.get(BooleanKey.EQ_DISPLAY) || !isEqScreen(title)) return;
        
        isEqScreen = true;
        currentEqScreen = screen;

        String taskName = "eq_scan_" + System.currentTimeMillis();
        RunDelayed.run(() -> {
            if (isEqScreen && currentEqScreen == screen) {
                scanEquipmentSlots(screen, true);
            }
        }, 200L, taskName);
    } 
    
    public static boolean isEqScreen(String title) {
        return title.startsWith("(") && title.endsWith("Equipment Sets");
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
    
    private static void scanEquipmentSlots(ContainerScreen screen, boolean forceUpdate) {
        if (!Config.get(BooleanKey.EQ_DISPLAY)) return;

        var handler = screen.getMenu();
        if (handler == null) return;

        int activeColumn = findActiveColumn(handler);
        if (activeColumn < 0) return;

        for (int row = 0; row < ARMOR_ROWS; row++) {
            int slotIndex = row * WARDROBE_COLUMNS + activeColumn;

            if (slotIndex >= 0 && slotIndex < handler.slots.size()) {
                Slot slot = handler.slots.get(slotIndex);
                var currentStack = slot.getItem();
                var lastStack = lastSeenStacks.get(row);
                
    
                if (forceUpdate || !ItemStack.matches(currentStack, lastStack)) {
                    update(currentStack, row);
                }
            }
        }
    }

    /**
     * Find the column of the currently active set
     *
     * @return the active column index (0-8), or -1 if none is active
     */
    private static int findActiveColumn(AbstractContainerMenu handler) {
        for (int col = 0; col < WARDROBE_COLUMNS; col++) {
            int statusSlotIndex = STATUS_ROW * WARDROBE_COLUMNS + col;
            if (statusSlotIndex >= handler.slots.size()) continue;

            var statusStack = handler.slots.get(statusSlotIndex).getItem();
            if (!statusStack.isEmpty() && statusStack.getItem() == McApi.getLimeDye()) {
                return col;
            }
        }
        return -1;
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
