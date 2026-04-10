package me.valkeea.fishyaddons.vconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.valkeea.fishyaddons.vconfig.core.UICategory;

/**
 * Adds a button that opens another screen for advanced configuration.
 * Can be standalone or paired with a toggle.
 * 
 * <h4>Usage example:</h4>
 * <pre>
 * // Redirect
 * {@literal @}UIRedirect(
 *      name {@literal =} "Redirect Example",
 *      method {@literal =} "foo",
 *      buttonText {@literal =} "Manage"
 * )
 * private boolean redirectExample;
 * 
 * // Toggle + HUD Redirect
 * {@literal @}UIToggle(
 *      key {@literal =} ConfigKey.HUD,
 *      name {@literal =} "HUD Example"
 * )
 * {@literal @}UIRedirect(method {@literal =} "foo")
 * private boolean hudExample;
 * 
 * private Screen foo() {
 *  // In the module class
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UIRedirect {

    // -- Core Metadata --

    /** 
     * Method name to invoke on click.
     * 
     * The method must be in the same class as the annotated field.
     */
    String method();    
    
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
     * Text displayed on the redirect button.
     */
    String buttonText() default "Configure";
}
