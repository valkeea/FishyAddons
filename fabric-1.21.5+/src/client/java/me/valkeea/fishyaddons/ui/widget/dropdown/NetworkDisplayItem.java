package me.valkeea.fishyaddons.ui.widget.dropdown;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.handler.NetworkMetrics;

public class NetworkDisplayItem implements ToggleMenuItem {
    private final String displayKey;

    public NetworkDisplayItem(String displayKey) {
        this.displayKey = displayKey;
    }
    
    @Override
    public String getId() {
        return displayKey;
    }
    
    @Override
    public String getDisplayName() {
        return displayKey.replace("pingHudShow", "").toUpperCase();
    }
    
    @Override
    public boolean isEnabled() {
        return FishyConfig.getState(displayKey, true);
    }
    
    @Override
    public void toggle() {
        FishyConfig.toggle(displayKey, true);
    }
}