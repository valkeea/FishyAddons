package me.valkeea.fishyaddons.vconfig.core;

public class ConfigModuleException extends RuntimeException {
    
    /**
     * Creates a new exception with the specified message.
     * @param message the detail message
     */
    public ConfigModuleException(String message) {
        super(message);
    }
    
    /**
     * Creates a new exception with the specified message and cause.
     * @param message the detail message
     * @param cause the underlying cause
     */
    public ConfigModuleException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new exception with the specified cause.
     * @param cause the underlying cause
     */
    public ConfigModuleException(Throwable cause) {
        super(cause);
    }
}
