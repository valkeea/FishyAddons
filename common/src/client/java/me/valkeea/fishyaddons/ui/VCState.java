package me.valkeea.fishyaddons.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages state preservation for VCScreen
 */
public class VCState {
    
    // Static state preservation for screen refreshes and reopening
    private static int preservedScrollOffset = 0;
    private static String preservedSearchText = "";
    private static Map<String, Boolean> preservedExpandedEntries = new HashMap<>();
    
    // Persistent state for complete closure
    private static int persistentScrollOffset = 0;
    private static String persistentSearchText = "";
    private static Map<String, Boolean> persistentExpandedEntries = new HashMap<>();

    // Temporary preservation
    public static void preserveState(int scrollOffset, String searchText, Map<String, Boolean> expandedEntries) {
        preservedScrollOffset = scrollOffset;
        preservedSearchText = searchText != null ? searchText : "";
        preservedExpandedEntries = new HashMap<>(expandedEntries != null ? expandedEntries : new HashMap<>());
    }

    // Persistent preservation
    public static void preservePersistentState(int scrollOffset, String searchText, Map<String, Boolean> expandedEntries) {
        persistentScrollOffset = scrollOffset;
        persistentSearchText = searchText != null ? searchText : "";
        persistentExpandedEntries = new HashMap<>(expandedEntries != null ? expandedEntries : new HashMap<>());
        preserveState(scrollOffset, searchText, expandedEntries);
    }

    // search setter
    public static void setLastSearchText(String searchText) {
        preservedSearchText = searchText != null ? searchText : "";
    }

    // -- Getters for preserved state
    public static int getLastScrollOffset() {
        if (preservedScrollOffset != 0 || hasTemporaryState()) {
            return preservedScrollOffset;
        }
        return persistentScrollOffset;
    }
    
    public static String getLastSearchText() {
        if (!preservedSearchText.isEmpty() || hasTemporaryState()) {
            return preservedSearchText;
        }
        return persistentSearchText;
    }
    
    public static Map<String, Boolean> getLastExpandedEntries() {
        if (!preservedExpandedEntries.isEmpty() || hasTemporaryState()) {
            return new HashMap<>(preservedExpandedEntries);
        }
        return new HashMap<>(persistentExpandedEntries);
    }

    // -- State management --
    public static void clearTemporaryState() {
        preservedScrollOffset = 0;
        preservedSearchText = "";
        preservedExpandedEntries.clear();
    }
    
    private static boolean hasTemporaryState() {
        return preservedScrollOffset != 0 || !preservedSearchText.isEmpty() || !preservedExpandedEntries.isEmpty();
    }
    
    public static void clearState() {
        preservedScrollOffset = 0;
        preservedSearchText = "";
        preservedExpandedEntries.clear();
        persistentScrollOffset = 0;
        persistentSearchText = "";
        persistentExpandedEntries.clear();
    }
    
    public static boolean hasPreservedState() {
        return hasTemporaryState() || hasPersistentState();
    }
    
    private static boolean hasPersistentState() {
        return persistentScrollOffset != 0 || !persistentSearchText.isEmpty() || !persistentExpandedEntries.isEmpty();
    }

    private VCState() {
        throw new UnsupportedOperationException("Utility class");
    }    
}
