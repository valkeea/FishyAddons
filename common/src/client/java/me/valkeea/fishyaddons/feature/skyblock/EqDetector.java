package me.valkeea.fishyaddons.feature.skyblock;

import java.util.HashMap;
import java.util.Map;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.tool.RunDelayed;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EqDetector {  
    private EqDetector() {}  

    private static final int[] HC_EQ_SLOTS = {10, 19, 28, 37};
    private static final int WARDROBE_COLUMNS = 9;
    private static final int ARMOR_ROWS = 4;
    private static final int STATUS_ROW = 4;

    private static final Map<Integer, ItemStack> lastSeenStacks = new HashMap<>();

    private static final String WD_SUFFIX = "Equipment Sets";
    private static final String LD_SUFFIX = "Loadouts";
    private static final String EQ_TITLE = "Stats & Equipment";

    private static ContainerScreen currentEqScreen = null;
    private static boolean isEqScreen = false;    
    private static int lastActiveColumn = -1;    

    public static void init() {
        FaEvents.SCREEN_OPEN.register(e -> onScreen(e.screen, e.titleString));
        FaEvents.SCREEN_CLOSE.register(e -> onScreenClosed(e.titleString));
    }

    public static void onScreen(ContainerScreen screen, String title) {
        if (!Config.get(BooleanKey.EQ_DISPLAY) || !isEqScreen(title)) return;
        
        int screenType = determineType(title);
        if (screenType == -1) return;

        isEqScreen = true;
        currentEqScreen = screen;

        var taskName = "eq_scan_" + System.currentTimeMillis();
        RunDelayed.run(() -> {
            if (isEqScreen && currentEqScreen == screen) {
                scanForEq(screen, screenType, true);
            }
        }, 200L, taskName);
    } 
    
    private static boolean isEqScreen(String title) {
        return title.contains("Equipment") || title.contains("Loadouts");
    }

    private static int determineType(String title) {
        if (title.endsWith(WD_SUFFIX)) return 0;
        if (title.endsWith(LD_SUFFIX) || title.endsWith(EQ_TITLE)) return 1;
        return -1;
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
            scanForEq(currentEqScreen, determineType(currentEqScreen.getTitle().getString()), true);
        }
    }
    
    private static void scanForEq(ContainerScreen screen, int type, boolean forceUpdate) {
        if (!Config.get(BooleanKey.EQ_DISPLAY)) return;

        var handler = screen.getMenu();
        if (handler == null) return;

        if (type == 0) {
            scanWardrobe(handler, forceUpdate);
        } else scanDefault(handler, forceUpdate);
    }

    private static void scanWardrobe(AbstractContainerMenu handler, boolean f) {

        int activeCol = findActiveColumn(handler);
        if (activeCol < 0) {
            if (checkIfUnequipped(handler)) {
                lastSeenStacks.clear();
                EqTextures.clearAll();
            } else return;
        }

        for (int row = 0; row < ARMOR_ROWS; row++) {
            int slotIndex = row * WARDROBE_COLUMNS + activeCol;
            checkSlot(handler, slotIndex, row, f);
        }
    }

    private static void scanDefault(AbstractContainerMenu handler, boolean f) {

        for (int i = 0; i < HC_EQ_SLOTS.length; i++) {
            int slotIndex = HC_EQ_SLOTS[i];
            checkSlot(handler, slotIndex, i, f);
        }
    }

    private static void checkSlot(AbstractContainerMenu handler, int slotIdx, int row, boolean forceUpdate) {
        
        if (slotIdx >= 0 && slotIdx < handler.slots.size()) {
            Slot slot = handler.slots.get(slotIdx);
            var currentStack = slot.getItem();
            var lastStack = lastSeenStacks.get(row);
            

            if (forceUpdate || !ItemStack.matches(currentStack, lastStack)) {
                update(currentStack, row);
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
                lastActiveColumn = col;
                return col;
            }
        }

        return -1;
    }

    /**
     * Check if the currently active set was unequipped (gray dye in status slot)
     */
    private static boolean checkIfUnequipped(AbstractContainerMenu handler) {
        if (lastActiveColumn < 0) return false;

        int statusSlotIndex = STATUS_ROW * WARDROBE_COLUMNS + lastActiveColumn;
        if (statusSlotIndex >= handler.slots.size()) return false;

        var statusStack = handler.slots.get(statusSlotIndex).getItem();
        return !statusStack.isEmpty() && statusStack.getItem() == McApi.getGrayDye();
    }

    private static void update(ItemStack currentStack, int i) {
        if (currentStack == null || currentStack.isEmpty()) {
            lastSeenStacks.remove(i);
            EqTextures.saveEmptySlot(i);

        } else {
            lastSeenStacks.put(i, currentStack.copy());
            var type = currentStack.getItem();
            boolean isSkull = type == Items.PLAYER_HEAD;
            if (!isSkull && type != Items.PAPER) {
                EqTextures.saveEmptySlot(i);
            } else {
                EqTextures.saveEqTexture(currentStack, i, isSkull);
            }
        }
    }
}
