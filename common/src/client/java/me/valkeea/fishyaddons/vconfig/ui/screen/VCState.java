package me.valkeea.fishyaddons.vconfig.ui.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.model.VCEntry;

public final class VCState {
    private static StateSnapshot savedState = empty();

    public record StateSnapshot(
        int offset, String searchText, UICategory category,
        List<VCEntry> filtered, Map<String, Boolean> expanded
    ) {}

    public static void preserveState(StateSnapshot snapshot) {
        savedState = snapshot;
    }

    public static StateSnapshot getPreviousState() {
        return savedState != null ? savedState : empty();
    }    

    public static void clear() {
        savedState = empty();
    }

    private static StateSnapshot empty() {
        return new StateSnapshot(0, "", null, new ArrayList<>(), new HashMap<>());
    }

    public static void setSearchTo(String searchText) {
        boolean wasSaved = savedState != null;
        savedState = new StateSnapshot(
            wasSaved ? savedState.offset() : 0,
            searchText,
            null,
            new ArrayList<>(),
            wasSaved ? savedState.expanded() : new HashMap<>()
        );
    }

    private VCState() {}    
}
