package me.valkeea.fishyaddons.ui.widget.dropdown;

import net.minecraft.text.Text;

/**
 * Interface for items that can be toggled on/off in a dropdown
 */
public interface ToggleMenuItem {
    /**
     * @return The unique identifier for this item
     */
    String getId();
    
    /**
     * @return The display name for this item
     */
    String getDisplayName();
    
    /**
     * @return True if this item is currently enabled/shown
     */
    boolean isEnabled();
    
    /**
     * Toggle the enabled state of this item
     */
    void toggle();
    
    default Text getEnabledSuffix() {
        Text checkMark = Text.literal("✓").styled(style -> style.withColor(0xCCFFCC).withBold(true));
        return Text.literal(" §8[").append(checkMark).append(Text.literal("§8]"));
    }
    
    default Text getDisabledSuffix() {
        Text crossMark = Text.literal("✗").styled(style -> style.withColor(0xFF8080).withBold(true));
        return Text.literal(" §8[").append(crossMark).append(Text.literal("§8]"));
    }
}