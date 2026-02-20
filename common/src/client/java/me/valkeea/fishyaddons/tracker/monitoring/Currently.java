package me.valkeea.fishyaddons.tracker.monitoring;

import java.util.Optional;

/**
 * Defines activity types for hierarchical tracking.
 */
public enum Currently {

    SKILL(null),
    FISHING(SKILL),
    DIANA(null),
    SPOOKY(null),
    SHARK(null),
    JERRY(null),
    SLAYER(null),
    DUNGEON(null);
    
    private final Currently parent;
    
    Currently(Currently parent) {
        this.parent = parent;
    }
    
    /**
     * Get the parent activity if this is a subactivity
     */
    public Optional<Currently> getParent() {
        return Optional.ofNullable(parent);
    }
    
    /**
     * Check if this is a subactivity
     */
    public boolean isSubActivity() {
        return parent != null;
    }
    
    /**
     * Check if this activity is or is a child of the given activity
     */
    public boolean isOrChildOf(Currently other) {
        if (this == other) {
            return true;
        }
        return parent != null && parent.isOrChildOf(other);
    }
}
