package me.valkeea.fishyaddons.vconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.api.StringKey;

/**
 * Searchable dropdown menu for String fields.
 * <p>
 * Intended for a single setting with multiple options, as an alternative to sliders.
 * <p>
 * <i>String field dropdowns can only ever be lone primary controls for optimal layout.
 * <br>They provide their own toggle functionality by
 * <br>resetting the config value when the field is double-clicked.</i>
 * 
 * <h4>Example:</h4>
 * <pre>
 * // Search Dropdown
 * {@literal @}UISearch(
 *      key {@literal =} ConfigKey.SEARCH,
 *      name {@literal =} "Search Example",
 *      provider {@literal =} "foo",
 * )
 * private String searchExample;
 * 
 * private List{@literal <}ToggleMenuItem{@literal >} foo() {
 *  // In the module class
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UISearch {
    
    // -- Core Metadata --
    
    /** 
     * ConfigKey this field is bound to.
     * Default value must be of type String.
     */
    StringKey key();

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
