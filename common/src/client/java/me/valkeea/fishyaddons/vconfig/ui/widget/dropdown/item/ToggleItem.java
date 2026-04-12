package me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item;

import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;

public class ToggleItem implements ToggleMenuItem {
    private final BooleanKey key;
    private final String displayName;

    public ToggleItem(BooleanKey key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }
    
    @Override
    public String getId() {
        return key.getString();
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public boolean isEnabled() {
        return Config.get(key);
    }
    
    @Override
    public void toggle() {
        Config.toggle(key);
    }
}
