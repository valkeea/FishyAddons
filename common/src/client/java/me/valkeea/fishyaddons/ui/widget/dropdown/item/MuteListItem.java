package me.valkeea.fishyaddons.ui.widget.dropdown.item;

import me.valkeea.fishyaddons.config.FishyConfig;

public class MuteListItem implements ToggleMenuItem {
    private final String displayKey;

    public MuteListItem(String displayKey) {
        this.displayKey = displayKey;
    }
    
    @Override
    public String getId() {
        return displayKey;
    }
    
    @Override
    public String getDisplayName() {
        return displayKey.replace("mute", "") + "s";
    }
    
    @Override
    public boolean isEnabled() {
        return FishyConfig.getState(displayKey, false);
    }
    
    @Override
    public void toggle() {
        FishyConfig.toggle(displayKey, false);
    }
}
