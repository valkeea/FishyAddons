package me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item;

import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;

public class NetworkDisplayItem implements ToggleMenuItem {
    private final BooleanKey displayKey;

    public NetworkDisplayItem(BooleanKey displayKey) {
        this.displayKey = displayKey;
    }
    
    @Override
    public String getId() {
        return displayKey.getString();
    }
    
    @Override
    public String getDisplayName() {
        return getId().replace("pingHudShow", "").toUpperCase();
    }
    
    @Override
    public boolean isEnabled() {
        return Config.get(displayKey);
    }
    
    @Override
    public void toggle() {
        Config.toggle(displayKey);
    }
}
