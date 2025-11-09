package me.valkeea.fishyaddons.ui.widget.dropdown;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.feature.waypoints.ChainConfig;
import me.valkeea.fishyaddons.feature.waypoints.WaypointChains;

public class WaypointChainItem implements ToggleMenuItem {
    private final String configKey;
    private final String displayName;
    private final String description;

    public WaypointChainItem(String configKey, String displayName, String description) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.description = description;
    }
    
    @Override
    public String getId() {
        return configKey;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean isEnabled() {
        return FishyConfig.getState(configKey, false);
    }
    
    @Override
    public void toggle() {
        FishyConfig.toggle(configKey, false);
        WaypointChains.refresh();
    }
    
    @Override
    public boolean onRightClick() {
        if (configKey != null) {
            ChainConfig.clearPresetFor(configKey);
            me.valkeea.fishyaddons.util.FishyNotis.notice("Reset waypoint completion for " + displayName);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean supportsRightClick() {
        return true;
    }
}
