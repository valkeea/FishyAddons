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
    
    /**
     * Handle right-click on this item. Default implementation does nothing.
     * @return true if the right-click was handled, false otherwise
     */
    default boolean onRightClick() {
        return false;
    }
    
    /**
     * @return true if this item supports right-click actions
     */
    default boolean supportsRightClick() {
        return false;
    }
    
    default Text getEnabledSuffix() {
        var checkMark = Text.literal("✓").styled(style -> style.withColor(0xFFCCFFCC).withBold(true));
        return Text.literal(" §8[").append(checkMark).append(Text.literal("§8]"));
    }
    
    default Text getDisabledSuffix() {
        var crossMark = Text.literal("✗").styled(style -> style.withColor(0xFFFF8080).withBold(true));
        return Text.literal(" §8[").append(crossMark).append(Text.literal("§8]"));
    }
}
