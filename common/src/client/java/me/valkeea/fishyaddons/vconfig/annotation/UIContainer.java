package me.valkeea.fishyaddons.vconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;

/**
 * Marks a config entry as an expandable container for sub-entries.
 * <p>
 * <i>If linked to a ConfigKey, this entry will also include a toggle button.</i>
 * 
 * <h4>Usage example:</h4>
 * <pre>
 * // Expandable container (no key)
 * {@literal @}UIContainer(
 *      name {@literal =} "Expandable Example",
 *      tooltip {@literal =} {"Add tooltip", "Line 2", "Line 3"}
 * )
 * private final boolean expandableExample = false;
 * 
 * // Sub-entry in the container, only visible in expanded state
 * {@literal @}UIToggle(
 *      key {@literal =} BooleanKey.SUB_ENTRY,
 *      name {@literal =} "Sub Entry",
 *      parent {@literal =} "Expandable Example" // Link to container
 * )
 * private boolean subEntry;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UIContainer {
    
    // -- Core Metadata --
    
    /** 
     * BooleanKey this field is bound to, optional.
     * 
     * If specified, the container will include a toggle button that enables/disables the linked setting.
     */
    BooleanKey key() default BooleanKey.NONE;
    
    /**
     * Display name in UI (uses field name if not specified)
     * Substrings in between *asterisks* will be used to generate tab items.
     */
    String name() default "";
    
    //--- Additional ---

    /** Description shown in UI */
    String[] description() default {};
    
    /** Display order within category (lower = first) */
    int order() default 100;    

    /**
     * Tooltip text for this entry. Uses default formatting for dynamic list.
     * Lines starting with '§' will be treated as custom format.
     */
    String[] tooltip() default {};

    /** Category for grouping (overrides module category) */
    UICategory category() default UICategory.NONE;

    /** Subcategory for grouping within parent entries */
    String subcategory() default "";
}
