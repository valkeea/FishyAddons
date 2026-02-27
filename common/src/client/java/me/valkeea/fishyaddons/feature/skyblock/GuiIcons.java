package me.valkeea.fishyaddons.feature.skyblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.mixin.HandledScreenAccessor;
import me.valkeea.fishyaddons.util.ContainerScanner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class GuiIcons {
    private GuiIcons() {}
    
    private static final Set<String> screenNames = new HashSet<>();
    private static final Map<String, Set<Integer>> screenSlotMap = new HashMap<>();
    private static List<Integer> cachedSlots = Collections.emptyList();
    private static String lastScreen = null;    
    private static boolean enabled = false;

    public static void init() {
        FaEvents.SCREEN_MOUSE_CLICK.register(event -> {
            if (checkClick(event.hoveredSlot, event.click.button(), event.screen)) {
                event.setConsumed(true);
            }
        }, EventPriority.NORMAL, EventPhase.PRE);
    }
    
    public static void onConfigLoaded() {
        screenNames.clear();
        screenNames.addAll(ItemConfig.getGuiIconsScreenNames());
        
        screenSlotMap.clear();
        Map<String, Set<Integer>> loaded = ItemConfig.getGuiIconsScreenSlotMap();
        for (var entry : loaded.entrySet()) {
            screenSlotMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        
        refresh();
    }

    private static boolean checkClick(Slot hovered, int button, HandledScreen<?> screen) {
        if (hovered == null || !anyBlocked()) return false;
        if (handleIcons(hovered)) { return true; }

        if (handleShift(hovered.id)) {
            int keyCode = 340;
            var cl = MinecraftClient.getInstance();
            
            if (cl.options != null) {
                keyCode = cl.options.sneakKey.getDefaultKey().getCode();
            }

            boolean shiftDown = InputUtil.isKeyPressed(cl.getWindow(), keyCode);

            if (shiftDown) {
                ((HandledScreenAccessor) screen).callOnMouseClick(hovered, hovered.id, button, SlotActionType.PICKUP);
                return true;
            }
        }
        return false;
    }

    private static boolean handleIcons(Slot hovered) {
        return isBlocked(hovered.id);
    }

    public static void refresh() {
        String keyCode = me.valkeea.fishyaddons.config.FishyConfig.getKeyString(
            me.valkeea.fishyaddons.config.Key.MOD_KEY_LOCK_GUISLOT);
        enabled = !screenNames.isEmpty() && !screenSlotMap.isEmpty() && keyCode != null && !keyCode.equals("NONE");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    private static boolean anyBlocked() {
        return hasConfig(ContainerScanner.current());
    }

    public static boolean hasConfig(String name) {
        if (!enabled) return false;
        return !screenNames.isEmpty() && checkGui(name);
    }

    private static boolean checkGui(String gui) {
        for (String name : screenNames) {
            if (gui.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }  

    public static boolean isBlocked(int slotIndex) {
        if (!enabled) return false;
        return getSlotsFor(ContainerScanner.current()).contains(slotIndex)
               && !isShiftDown(MinecraftClient.getInstance());
    }

    public static boolean handleShift(int slotIndex) {
        return getSlotsFor(ContainerScanner.current()).contains(slotIndex);
    }    

    private static boolean isShiftDown(MinecraftClient cl) {
        if (cl.options == null) return false;
        int keyCode = cl.options.sneakKey.getDefaultKey().getCode();
        return InputUtil.isKeyPressed(cl.getWindow(), keyCode);
    }   

    /**
     * Add a slot to the map for the given screen name.
     * If the screen is not already registered, it will be added to the configured list.
     */
    public static void addGuiSlot(String screenName, int slotId) {

        String key = screenName.toLowerCase(java.util.Locale.ROOT);
        Set<Integer> slots = screenSlotMap.get(key);
        boolean exists = slots != null && slots.contains(slotId);
        
        if (exists) {
            slots.remove(slotId);
            if (slots.isEmpty()) {
                screenSlotMap.remove(key);
                screenNames.removeIf(name -> name.equalsIgnoreCase(screenName));
                ItemConfig.removeGuiIconsScreen(screenName);
            } else {
                ItemConfig.removeGuiIconsSlot(screenName, slotId);
            }

        } else {
            if (!screenNames.contains(screenName)) {
                screenNames.add(screenName);
                ItemConfig.addGuiIconsScreen(screenName);
            }
            screenSlotMap.computeIfAbsent(key, k -> new HashSet<>()).add(slotId);
            ItemConfig.addGuiIconsSlot(screenName, slotId);
        }
        
        refresh();
        refreshSlots(screenName);
        ItemConfig.saveGuiIcons();     
    }

    /**
     * Returns a list of slot indices for the given screen name, cached for latest
     */    
    public static List<Integer> slots(String screen) {
        Set<Integer> slots = screenSlotMap.getOrDefault(screen.toLowerCase(Locale.ROOT),
        Collections.emptySet());
        return new ArrayList<>(slots);
    }

    private static void refreshSlots(String screen) {
        if (screen.isEmpty()) return;
        lastScreen = screen;
        cachedSlots = slots(lastScreen);
    }

    public static List<Integer> getSlotsFor(String screen) {
        if (screen.isEmpty()) return Collections.emptyList();
        if (!screen.equals(lastScreen)) {
            refreshSlots(screen);
        }
        return cachedSlots;
    }
}
