package me.valkeea.fishyaddons.ui.widget.dropdown;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.config.StatConfig;
import me.valkeea.fishyaddons.tracker.fishing.Sc;
import me.valkeea.fishyaddons.tracker.fishing.ScRegistry;

public class ScItem implements ToggleMenuItem {
    private final String creatureKey;
    
    public ScItem(String creatureKey) {
        this.creatureKey = creatureKey;
    }
    
    @Override
    public String getId() {
        return creatureKey;
    }
    
    @Override
    public String getDisplayName() {
        return Sc.displayName(creatureKey);
    }
    
    @Override
    public boolean isEnabled() {
        return !StatConfig.isIgnoredSc(creatureKey);
    }
    
    @Override
    public void toggle() {
        boolean currentlyIgnored = StatConfig.isIgnoredSc(creatureKey);
        StatConfig.setIgnoredSc(creatureKey, !currentlyIgnored);
        ScRegistry.getInstance().getCreaturesFor(SkyblockAreas.getIsland(), true);
    }
}
