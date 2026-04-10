package me.valkeea.fishyaddons.vconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import me.valkeea.fishyaddons.vconfig.core.UICategory;

/**
 * Marks a class as a configuration module. {@link UICategory} should be specified if the
 * <br>module contains UI annotations.
 * <h4>Classes with this annotation will be automatically:</h4>
 * <ul>
 * <li>Scanned for {@link VCListener}, {@link VCInit} and any UI annotations</li>
 * <li>Registered with the config system</li>
 * </ul>
 * 
 * <h4>UI generation usage patterns:</h4>
 * <ul>
 * <li> Simple controls: One annotation linked to a field</li>
 * <li> Special/Composite controls: Two annotations linked to the same field</li>
 * </ul>
 * 
 * <h4>Control Types:</h4>
 * <ul>
 * <li>{@link UIToggle} - Toggle button, default ON/OFF label.</li>
 * <li>{@link UIExtraToggle} - A secondary-only toggle tied to primary metadata</li>
 * <li>{@link UIColorPicker} - Color picker button with preview</li>
 * <li>{@link UISlider} - Slider control for <b>int/double</b> fields</li>
 * <li>{@link UISearch} - Searchable dropdown for <b>String</b> fields</li>
 * <li>{@link UIDropdown} - Dropdown menu for multiple <b>boolean</b> options</li>
 * <li>{@link UIContainer} - Expandable container with optional toggle</li>
 * <li>{@link UIRedirect} - Button to invoke a custom action</li>
 * <li>{@link UIHudRedirect} - Opens HUD configuration screen for the linked element</li>
 * <li>{@link UIKeybind} - Keybind input</li>
 * </ul>
 * 
 * <h4>Multiple Annotations (composite controls)</h4>
 * 
 * <i>For composite controls, declaration order determines primary and secondary control.</i>
 * <br>
 * <i>Secondary controls should only include hidden metadata.</i>
 * <h4>Currently supported combinations, as primary-secondary:</h4>
 * <ul>
 * <li> {@link UIToggle} + Any except string dropdown.</li>
 * <li> {@link UISlider} + {@link UIColorPicker}.</li>
 * <li> {@link UIColorPicker} + {@link UIExtraToggle} or {@link UIHudRedirect}.</li>
 * <li> {@link UIDropdown} and {@link UIContainer} add a toggle based on metadata</li>
 * </ul>
 * <br>
 * <i>String field dropdowns can only ever be singular for layout reasons. They provide their own
 * toggle functionality by resetting the config value when the field is double-clicked.</i>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface VCModule {
    /**
     * Category for this module (used for UI organization and display name)
     */
    UICategory value() default UICategory.NONE;
    
    /**
     * Whether this module should be auto-initialized
     */
    boolean autoInit() default true;
}
