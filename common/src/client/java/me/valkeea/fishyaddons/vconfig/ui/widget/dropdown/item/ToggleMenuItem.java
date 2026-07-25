package me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item;

import net.minecraft.network.chat.Component;

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
    default void toggle() {}

    /**
     * Handle right-click on this item. Default implementation does nothing.
     * @return true if the right-click was handled, false otherwise
     */
    default boolean onRightClick() {
        return false;
    }
    
    /**
     * @return true if this item supports right-click actions.
     */
    default boolean supportsRightClick() {
        return false;
    }
    
    /**
     * @return true if this item should use fixed width with scrolling text overflow.
     */
    default boolean useFixedWidth() {
        return false;
    }
    
    default Component getEnabledSuffix() {
        var checkMark = Component.literal("✓").withStyle(style -> style.withColor(0xFFCCFFCC).withBold(true));
        return Component.literal(" §8[").append(checkMark).append(Component.literal("§8]"));
    }
    
    default Component getDisabledSuffix() {
        var crossMark = Component.literal("✗").withStyle(style -> style.withColor(0xFFFF8080).withBold(true));
        return Component.literal(" §8[").append(crossMark).append(Component.literal("§8]"));
    }
}
