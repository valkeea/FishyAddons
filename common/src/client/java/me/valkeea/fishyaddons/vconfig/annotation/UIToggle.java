package me.valkeea.fishyaddons.vconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;

/**
 * Simple toggle control for boolean config values.
 * 
 * <h4>Usage example:</h4>
 * <pre>
 * {@literal @}UIToggle(
 *      key {@literal =} BooleanKey.EXAMPLE,
 *      name {@literal =} "Example Toggle",
 *      autoSync {@literal =} true // Field will automatically sync with config value
 * )
 * private static boolean exampleToggle;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UIToggle {

    // -- Core Metadata --
    
    /** 
     * ConfigKey this field is bound to.
     */
    BooleanKey key();
    
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
     * Text displayed on the toggle button,
     * defaults to dynamic ON/OFF if left empty
     */
    String buttonText() default "";
    
    /** 
     * If set to true, the field will automatically sync
     * with the config key value using reflection.
     */
    boolean autoSync() default false;
}
