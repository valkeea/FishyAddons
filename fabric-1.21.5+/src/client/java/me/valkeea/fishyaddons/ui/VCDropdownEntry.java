package me.valkeea.fishyaddons.ui;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.tracker.fishing.Sc;
import me.valkeea.fishyaddons.ui.widget.dropdown.MuteListItem;
import me.valkeea.fishyaddons.ui.widget.dropdown.NetworkDisplayItem;
import me.valkeea.fishyaddons.ui.widget.dropdown.ScItem;
import me.valkeea.fishyaddons.ui.widget.dropdown.ToggleMenuItem;

/**
 * Utility class for creating dropdown entries for toggle sets
 */
public class VCDropdownEntry {
    
    /**
     * Sc Display toggles
     */
    public static VCEntry scDisplayToggle(String name, String description, String toggleKey, boolean defaultValue, Runnable refreshAction) {
        return VCEntry.toggleDropdown(
            name,
            description, 
            toggleKey,
            defaultValue,
            "Configure",
            VCDropdownEntry::getScItems,
            refreshAction
        );
    }

    /**
     * Network Display toggles
     */
    public static VCEntry networkDisplayToggle(String name, String description, String toggleKey, boolean defaultValue, Runnable refreshAction) {
        return VCEntry.toggleDropdown(
            name,
            description, 
            toggleKey,
            defaultValue,
            "Configure",
            VCDropdownEntry::getNetworkDisplayItems,
            refreshAction
        );
    }

    public static List<ToggleMenuItem> getScItems() {
        List<ToggleMenuItem> items = new ArrayList<>();
        List<String> trackedCreatures = Sc.getTrackedCreatures();
        
        for (String creatureKey : trackedCreatures) {
            items.add(new ScItem(creatureKey));
        }
        
        return items;
    }    

    private static List<ToggleMenuItem> getNetworkDisplayItems() {
        List<ToggleMenuItem> items = new ArrayList<>();
        items.add(new NetworkDisplayItem(Key.HUD_PING_SHOW_PING));
        items.add(new NetworkDisplayItem(Key.HUD_PING_SHOW_TPS));
        items.add(new NetworkDisplayItem(Key.HUD_PING_SHOW_FPS));
        return items;
    }

    public static List<ToggleMenuItem> getMuteListItems() {
        List<ToggleMenuItem> items = new ArrayList<>();
        items.add(new MuteListItem(Key.MUTE_PHANTOM));
        items.add(new MuteListItem(Key.MUTE_RUNE));
        items.add(new MuteListItem(Key.MUTE_THUNDER));
        return items;
    }

    private VCDropdownEntry() {
        throw new UnsupportedOperationException("Utility class");
    }    
}