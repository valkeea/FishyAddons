package me.valkeea.fishyaddons.vconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.api.IntKey;

/**
 * Color preview with a redirect to open {@link me.valkeea.fishyaddons.vconfig.ui.screen.ColorWheel}.
 * 
 * <h4>Examples:</h4>
 * <pre>
 * // Toggle + Color Picker
 * {@literal @}UIToggle(key {@literal =} ConfigKey.BOOLEAN, name {@literal =} "Composite Example")
 * {@literal @}UIColorPicker(key {@literal =} ConfigKey.INT)
 * private boolean toggleWithColor;
 * 
 * // Standalone
 * {@literal @}UIColorPicker(key {@literal =} ConfigKey.INT2,
 *      name {@literal =} "Color Picker Example",
 *      description {@literal =} "Description for the setting"
 * )
 * private int colorValue;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UIColorPicker {
    
    // -- Core Metadata --
    
    /** 
     * ConfigKey this field is bound to.
     */
    IntKey key();
    
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
     * If set to true, the field will automatically sync
     * with the config key value using reflection.
     */
    boolean autoSync() default false;

    Class<?>[] dependsOn() default {};
}
