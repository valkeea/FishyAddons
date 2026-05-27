package me.valkeea.fishyaddons.vconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.api.StringKey;

/**
 * Marks a method to be called when specific config values change.
 * <h4>The method signature should be one of:</h4>
 * <ul>
 * <li>void foo(T newValue)</li>
 * <li>void foo()</li>
 * </ul>
 * <h4>Usage examples:</h4>
 * <pre>
 * // Single type listener with a boolean value
 * {@literal @}VCListener(BooleanKey.BOOL1)
 * private static void bar(boolean newValue) {
 *     fireAni = newValue;
 * }
 * 
 * // Mixed types
 * {@literal @}VCListener(
 *     value = BooleanKey.BOOL1,
 *     doubles = {DoubleKey.DBL1, DoubleKey.DBL2},
 * )
 * private static void baz() {
 *     // Called when any of the specified keys change
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VCListener {
    /**
     * Boolean config keys this listener responds to.
     */
    BooleanKey[] value() default {};
    
    /**
     * Integer config keys this listener responds to.
     */
    IntKey[] ints() default {};
    
    /**
     * Double config keys this listener responds to.
     */
    DoubleKey[] doubles() default {};
    
    /**
     * String config keys this listener responds to.
     */
    StringKey[] strings() default {};
    
    /**
     * Execution priority (lower = earlier).
     * Useful when listeners have dependencies.
     */
    int priority() default 100;
    
    /**
     * When this listener should be invoked.
     */
    Phase phase() default Phase.SYNC;
    
    /**
     * Listener execution phase.
     */
    enum Phase {
        
        /**
         * Called once on startup and every time the watched config value(s) change.
         */
        SYNC,

        /**
         * Called every time the watched config value(s) change.
         */
        CHANGE,
        
        /**
         * Called on change after all SYNC/CHANGE listeners have executed.
         */
        POST_CHANGE
    }
}
