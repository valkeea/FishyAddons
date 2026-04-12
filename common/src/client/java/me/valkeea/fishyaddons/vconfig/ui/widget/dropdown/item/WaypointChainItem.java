package me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item;

import me.valkeea.fishyaddons.feature.waypoints.ChainConfig;
import me.valkeea.fishyaddons.feature.waypoints.WaypointChains;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;

public class WaypointChainItem implements ToggleMenuItem {
    private final BooleanKey configKey;
    private final String displayName;
    private final String description;

    public WaypointChainItem(BooleanKey configKey, String displayName, String description) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.description = description;
    }
    
    @Override
    public String getId() {
        return configKey.getString();
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
        return Config.get(configKey);
    }
    
    @Override
    public void toggle() {
        Config.toggle(configKey);
        WaypointChains.refresh();
    }
    
    @Override
    public boolean onRightClick() {

        if (configKey != null) {

            ChainConfig.clearPresetFor(configKey.getString());
            me.valkeea.fishyaddons.util.FishyNotis.send("§aReset waypoint completion for " + displayName);
            return true;
        }

        return false;
    }
    
    @Override
    public boolean supportsRightClick() {
        return true;
    }
}
