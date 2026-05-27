package me.valkeea.fishyaddons.tracker.monitoring;

/**
 * Activity-specific save and pause operations.
 */
public interface ActivityHandler {
    
    /**
     * Save current session data for this activity.
     * Called periodically during active sessions.
     */
    void save();
    
    /**
     * Save session data and prepare for inactivity.
     * Called when activity has been inactive for the configured duration.
     * Default implementation is the same as save().
     */
    default void saveSession() {
        save();
    }
    
    /**
     * Set the paused state for this activity.
     * Called when activity becomes inactive for the configured pause duration.
     * 
     * @param paused true to pause, false to resume
     */
    void setPaused(boolean paused);
    
    /**
     * Check if this activity handler is currently enabled.
     * 
     * @return true if the activity should be tracked
     */
    boolean isEnabled();
}
