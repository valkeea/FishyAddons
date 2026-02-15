package me.valkeea.fishyaddons.feature.skyblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.mixin.HandledScreenAccessor;
import me.valkeea.fishyaddons.util.SbGui;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

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

    private static boolean checkClick(Slot hovered, int button, HandledScreen<?> screen) {
        if (hovered == null || !SbGui.getInstance().inGui()) return false;
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
        return GuiIcons.isBlocked(hovered.id);
    }

    public static void refresh() {
        String keyCode = me.valkeea.fishyaddons.config.FishyConfig.getKeyString(
            me.valkeea.fishyaddons.config.Key.MOD_KEY_LOCK_GUISLOT);
        enabled = !screenNames.isEmpty() && !screenSlotMap.isEmpty() && keyCode != null && !keyCode.equals("NONE");
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean hasConfig(Text title) {
        if (!enabled) return false;
        String screenName = title.getString();
        return !screenNames.isEmpty() && checkScreen(screenName);
    }

    private static boolean checkScreen(String screenName) {
        for (String name : screenNames) {
            if (screenName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }    

    public static boolean isBlocked(int slotIndex) {
        if (!enabled) return false;
        return getSlotsForScreen(SbGui.getInstance().current()).contains(slotIndex)
               && !isShiftDown(MinecraftClient.getInstance());
    }

    public static boolean handleShift(int slotIndex) {
        return getSlotsForScreen(SbGui.getInstance().current()).contains(slotIndex);
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
        if (!screenNames.contains(screenName)) {
            screenNames.add(screenName);
            SbGui.getInstance().setInGui(screenName);
        }
        java.util.Set<Integer> slots = screenSlotMap.computeIfAbsent(key, k -> new java.util.HashSet<>());
        boolean added = slots.add(slotId);
        if (!added) {
            removeGuiSlot(screenName, slotId);
            if (slots.isEmpty()) {
                screenNames.removeIf(name -> name.equalsIgnoreCase(screenName));
                screenSlotMap.remove(key);
            }
        }
        refresh();
        refreshSlots(screenName);
        me.valkeea.fishyaddons.config.ItemConfig.saveGuiIcons();     
    }

    private static void removeGuiSlot(String screenName, int slotIndex) {
        String key = screenName.toLowerCase(Locale.ROOT);
        Set<Integer> slots = screenSlotMap.get(key);
        if (slots != null) {
            slots.remove(slotIndex);
            if (slots.isEmpty()) {
                screenSlotMap.remove(key);
            }
        }
    }    

    /**
     * Returns a list of slot indices for the given screen name, cached for latest
     */    
    public static List<Integer> slots(String screenName) {
        Set<Integer> slots = screenSlotMap.getOrDefault(screenName.toLowerCase(Locale.ROOT),
        Collections.emptySet());
        return new ArrayList<>(slots);
    }

    private static void refreshSlots(String screen) {
        if (screen == null) return;
        lastScreen = screen;
        cachedSlots = slots(lastScreen);
    }

    public static List<Integer> getSlotsForScreen(String screenName) {
        if (screenName == null) return Collections.emptyList();
        if (!screenName.equals(lastScreen)) {
            refreshSlots(screenName);
        }
        return cachedSlots;
    }

    public static Set<String> getScreenNames() {
        return new HashSet<>(screenNames);
    }

    public static void setScreenNames(Set<String> names) {
        screenNames.clear();
        screenNames.clear();
        screenNames.addAll(names);
    }        

    public static Map<String, Set<Integer>> getScreenSlotMap() {
        Map<String, Set<Integer>> copy = new HashMap<>();
        for (Map.Entry<String, Set<Integer>> e : screenSlotMap.entrySet()) {
            copy.put(e.getKey(), new HashSet<>(e.getValue()));
        }
        return copy;
    }

    public static void setScreenSlotMap(Map<String, Set<Integer>> map) {
        screenSlotMap.clear();
        for (Map.Entry<String, Set<Integer>> e : map.entrySet()) {
            screenSlotMap.put(e.getKey(), new HashSet<>(e.getValue()));
        }
    }
}
