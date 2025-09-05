package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.util.EqTextures;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class EqDetector {
    
    private static boolean isEqScreen = false;
    @SuppressWarnings("rawtypes")
    private static HandledScreen currentEquipmentScreen = null;
    private static final int[] EQUIPMENT_SLOTS = {10, 19, 28, 37};

    private EqDetector() {}
    
    @SuppressWarnings("rawtypes")
    public static void onScreen(HandledScreen screen) {
        if (!FishyConfig.getState(Key.EQ_DISPLAY, false)) return;
        
        isEqScreen = true;
        currentEquipmentScreen = screen;
        extract(screen);
        
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(500);
                if (isEqScreen && currentEquipmentScreen == screen) {
                    extract(screen);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    public static boolean isEqScreen() {
        return isEqScreen;
    }

    public static void onScreenClosed() {
        isEqScreen = false;
        currentEquipmentScreen = null;
    }
    
    /**
     * Extract skull textures from equipment slots
     * @param screen detected equipment screen
     */
    @SuppressWarnings("rawtypes")
    private static void extract(HandledScreen screen) {
        if (!FishyConfig.getState(Key.EQ_DISPLAY, false)) return;
        
        var handler = screen.getScreenHandler();
        if (handler == null) {
            return;
        }
        
        for (int i = 0; i < EQUIPMENT_SLOTS.length; i++) {
            int slotIndex = EQUIPMENT_SLOTS[i];

            if (slotIndex >= 0 && slotIndex < handler.slots.size()) {
                Slot slot = handler.slots.get(slotIndex);
                ItemStack stack = slot.getStack();
                
                if (stack == null || stack.isEmpty()) {
                    EqTextures.saveEmptySlot(i);
                    continue;
                }

                if (stack.getItem().toString().contains("player_head")) {
                    EqTextures.saveSkullTexture(i, stack);
                }
            } else {
                System.out.println("  Invalid slot index: " + slotIndex + " (max: " + (handler.slots.size() - 1) + ")");
            }
        }
    }
}
