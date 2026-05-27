package me.valkeea.fishyaddons.vconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.valkeea.fishyaddons.vconfig.api.BooleanKey;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UIExtraToggle {

    // -- Core Metadata --
    
    /** 
     * ConfigKey this field is bound to.
     */
    BooleanKey key();
    
    //--- Additional --- 

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
