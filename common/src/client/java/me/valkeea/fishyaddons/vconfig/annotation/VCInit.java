package me.valkeea.fishyaddons.vconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to be called when the config system is initialized.
 * The method should only be used for setting up initial values or loading
 * processes and should not accept any parameters.
 * <h4>Example:</h4>
 * <pre>
 * {@literal @}VCInit
 * private static void foo() {
 *    // Initialization code here
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VCInit {
    // Marker annotation
}
