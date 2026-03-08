package me.valkeea.fishyaddons.tracker.collection;

import java.util.HashSet;
import java.util.Set;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.config.Key;

/**
 * Manages visibility state for collection items in the display.
 */
@SuppressWarnings("squid:S6548")
public class VisibilityManager {
    private static VisibilityManager instance;
    private final Set<String> hiddenCollections = new HashSet<>();
    
    private VisibilityManager() {
        load();
    }
    
    public static VisibilityManager getInstance() {
        if (instance == null) {
            instance = new VisibilityManager();
        }
        return instance;
    }
    
    /**
     * Check if a collection is hidden
     */
    public boolean isHidden(String itemName) {
        return hiddenCollections.contains(itemName);
    }
    
    /**
     * Toggle visibility of a collection
     * @return true if now hidden, false if now visible
     */
    public boolean toggleVisibility(String itemName) {
        boolean wasHidden = hiddenCollections.contains(itemName);
        if (wasHidden) {
            hiddenCollections.remove(itemName);
        } else {
            hiddenCollections.add(itemName);
        }
        save();
        return !wasHidden;
    }
    
    /**
     * Get all hidden collection names
     */
    public Set<String> getHiddenCollections() {
        return new HashSet<>(hiddenCollections);
    }
    
    private void load() {
        String hidden = ItemConfig.getString(Key.HIDDEN_COLLECTIONS, "");
        hiddenCollections.clear();
        if (!hidden.isEmpty()) {
            for (String item : hidden.split(",")) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    hiddenCollections.add(trimmed);
                }
            }
        }
    }
    
    private void save() {
        String hidden = String.join(",", hiddenCollections);
        ItemConfig.setString(Key.HIDDEN_COLLECTIONS, hidden);
    }
    
    public void reset() {
        hiddenCollections.clear();
        save();
    }
}
