package me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item;


import me.valkeea.fishyaddons.feature.item.safeguard.BlacklistManager;
import me.valkeea.fishyaddons.util.text.StringUtils;

public class BlacklistItem implements ToggleMenuItem {
    private final String identifier;
    private final String displayName;

    public BlacklistItem(String identifier) {
        this.identifier = identifier;
        this.displayName = formatIdentifier(identifier);
    }
    
    @Override
    public String getId() {
        return identifier;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public boolean isEnabled() {
        return BlacklistManager.isEnabled(identifier);
    }

    @Override
    public void toggle() {
        BlacklistManager.toggle(identifier);
    }

    private String formatIdentifier(String id) {
        return StringUtils.capitalize(id.replace("_", " "));
    }
}
