package me.valkeea.fishyaddons.feature.skyblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;

import me.valkeea.fishyaddons.event.EventPhase;
import me.valkeea.fishyaddons.event.EventPriority;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.mixin.HandledScreenAccessor;
import me.valkeea.fishyaddons.util.ContainerScanner;
import me.valkeea.fishyaddons.vconfig.annotation.VCInit;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import me.valkeea.fishyaddons.vconfig.config.impl.ItemConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

@VCModule
public class GuiIcons {
    private GuiIcons() {}
    
    private static final Set<String> screenNames = new HashSet<>();
    private static final Map<String, Set<Integer>> screenSlotMap = new HashMap<>();
    private static List<Integer> cachedSlots = Collections.emptyList();
    private static String lastScreen = null;    
    private static boolean enabled = false;

    @VCInit
    public static void init() {
        FaEvents.SCREEN_MOUSE_CLICK.register(event -> {
            if (checkClick(event.hoveredSlot, event.click.button(), event.screen)) {
                event.setConsumed(true);
            }
        }, EventPriority.LOWEST, EventPhase.POST);
        load();
    }
    
    private static void load() {
        screenNames.clear();
        screenNames.addAll(ItemConfig.getGuiIconsScreenNames());
        
        screenSlotMap.clear();
        Map<String, Set<Integer>> loaded = ItemConfig.getGuiIconsScreenSlotMap();
        for (var entry : loaded.entrySet()) {
            screenSlotMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        
        refresh();
    }

    private static boolean checkClick(Slot hovered, int button, AbstractContainerScreen<?> screen) {
        if (hovered == null || !anyBlocked()) return false;
        if (handleIcons(hovered)) { return true; }

        if (handleShift(hovered.index)) {
            int keyCode = 340;
            var cl = Minecraft.getInstance();
            
            if (cl.options != null) {
                keyCode = cl.options.keyShift.getDefaultKey().getValue();
            }

            boolean shiftDown = InputConstants.isKeyDown(cl.getWindow(), keyCode);

            if (shiftDown) {
                ((HandledScreenAccessor) screen).callOnMouseClick(hovered, hovered.index, button, ContainerInput.PICKUP);
                return true;
            }
        }
        return false;
    }

    private static boolean handleIcons(Slot hovered) {
        return isBlocked(hovered.index);
    }

    @VCListener(strings = StringKey.KEY_HIDE_GUI)    
    public static void refresh() {
        String keyCode = Config.get(StringKey.KEY_HIDE_GUI);
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
               && !isShiftDown(Minecraft.getInstance());
    }

    public static boolean handleShift(int slotIndex) {
        return getSlotsFor(ContainerScanner.current()).contains(slotIndex);
    }    

    private static boolean isShiftDown(Minecraft cl) {
        if (cl.options == null) return false;
        int keyCode = cl.options.keyShift.getDefaultKey().getValue();
        return InputConstants.isKeyDown(cl.getWindow(), keyCode);
    }   

    /**
     * Add a slot to the map for the given screen name.
     * If the screen is not already registered, it will be added to the configured list.
     */
    public static void addGuiSlot(String screenName, int slotId) {

        String key = screenName.toLowerCase(java.util.Locale.ROOT);
        Set<Integer> slots = screenSlotMap.get(key);
        
        if (slots != null && slots.contains(slotId)) {
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
