package me.valkeea.fishyaddons.vconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;

/**
 * Dropdown menu for boolean fields.
 * <p>
 * Key is not required, as the intent of this control is to provide multiple toggle options.
 * If one is provided, the dropdown will include a toggle button that enables/disables the linked setting.
 * 
 * <h4>Examples:</h4>
 * <pre>
 * // Toggle Dropdown
 * {@literal @}UIDropdown(
 *      name {@literal =} "Dropdown Example",
 *      provider {@literal =} "foo",
 *      buttonText {@literal =} "Configure"
 * )
 * private boolean dropdownExample;
 * 
 * private List{@literal <}ToggleMenuItem{@literal >} foo() {
 *  // In the module class
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UIDropdown {
    
    // -- Core Metadata --
    
    /** 
     * ConfigKey this field is bound to.
     */
    BooleanKey key() default BooleanKey.NONE;

    /** 
     * Method name that provides dropdown options.
     * 
     * Method signature: List<ToggleMenuItem> methodName()
     * 
     * The method must be in the same class as the annotated field.
     */
    String provider();    
    
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
     * Parent entry for hierarchical grouping, must exactly match the name of another entry.
     * All children are implicitly grouped under the parent in tab and UI generation.
     */
    String parent() default "";

    /**
     * Tooltip text for this entry. Uses default formatting for dynamic list.
     * Lines starting with '§' will be treated as custom format.
     */
    String[] tooltip() default {};

    /** Category for grouping (overrides module category) */
    UICategory category() default UICategory.NONE;

    /** Subcategory for grouping within parent entries */
    String subcategory() default "";
    
    /** 
     * Text displayed on the dropdown button.
     * Defaults to "Configure" if not specified.
     */
    String buttonText() default "Configure";
    
    /** 
     * If set to true, the field will automatically sync
     * with the config key value using reflection.
     */
    boolean autoSync() default false;
}
