package me.valkeea.fishyaddons.tracker.fishing;

import java.util.function.Supplier;

/**
 * Spawn pool requirement with a name and a condition to check if it's met.
 */
public class SpawnRequirement {
    private final String name;
    private final Supplier<Boolean> condition;
    
    public SpawnRequirement(String name, Supplier<Boolean> condition) {
        this.name = name;
        this.condition = condition;
    }
    
    public boolean isMet() {
        return condition.get();
    }
    
    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
