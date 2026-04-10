package me.valkeea.fishyaddons.vconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;

/**
 * Configurable slider control for a single double or integer value.
 * 
 * <h4>Usage examples:</h4>
 * <pre>
 * // Slider
 * {@literal @}UISlider(
 *      key {@literal =} DoubleKey.E1_SLIDER,
 *      name {@literal =} "Slider Example",
 *      min {@literal =} 0.0, max {@literal =} 2.0,
 *      labels {@literal =} {"Off", "Opt1", "Opt2"},
 * )
 * private float sliderExample;
 * 
 * // Toggle + Slider
 * {@literal @}UIToggle(key {@literal =} BooleanKey.E2, name {@literal =} "Toggle + Slider")
 * {@literal @}UISlider(
 *      altKey {@literal =} IntKey.E2_SLIDER,
 *      min {@literal =} 0, max {@literal =} 1,
 *      format {@literal =} "%s%%"
 * )
 * private boolean toggleAndSlider;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UISlider {
    
    // -- Core Metadata --
    
    /** 
     * ConfigKey this field is bound to.
     * Can be left as default if altKey is used instead.
     */
    DoubleKey key() default DoubleKey.NONE;

    /** 
     * Alternative IntKey for integer sliders.
     * If specified, this will be used instead of the DoubleKey.
     */
    IntKey altKey() default IntKey.NONE;
    
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
    
    /** Minimum value for slider */
    double min() default 0;
    
    /** Maximum value for slider */
    double max() default 100;
    
    /** 
     * Format string for slider value display (e.g., "%.1fx", "%.0f%%").
     * Uses Java String.format() syntax.
     */
    String format() default "%.0f";

    /** 
     * Value labels for discrete slider positions.
     * If provided, displays these labels instead of numeric values.
     * Array length will automatically determine the number of steps and step size.
     * If length is 1, label will only be used for the 0.0 value.
     * 
     * Example: valueLabels = {"Off", "Low", "Medium", "High"}
     */
    String[] labels() default {}; 
    
    /**
     * Optional label colors, corresponding to each entry in {@link #labels()}.
     * Colors should be in ARGB hex format (e.g., 0xFFFF0000 for red).
     */
    int[] labelColors() default {};
    
    /** 
     * If set to true, the field will automatically sync
     * with the config key value using reflection.
     */
    boolean autoSync() default false;
}
