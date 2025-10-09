package me.valkeea.fishyaddons.ui;

import java.util.ArrayList;
import java.util.List;

import me.valkeea.fishyaddons.tracker.fishing.Sc;
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
     * Get all Sc toggles
     */
    public static List<ToggleMenuItem> getScItems() {
        List<ToggleMenuItem> items = new ArrayList<>();
        List<String> trackedCreatures = Sc.getTrackedCreatures();
        
        for (String creatureKey : trackedCreatures) {
            items.add(new ScItem(creatureKey));
        }
        
        return items;
    }

    private VCDropdownEntry() {
        throw new UnsupportedOperationException("Utility class");
    }
}