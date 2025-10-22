package me.valkeea.fishyaddons.tracker;

import java.util.EnumSet;
import java.util.Set;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.tracker.fishing.ScStats;

public class ActivityMonitor {
    private static ActivityMonitor instance = null;

    private static final long PAUSE_DELAY_MS = 90000;
    private static final long SAVE_INTERVAL_MS = 300000;
    private static final long TICK_INTERVAL = 1000;
    
    private long lastActivityTime = 0;
    private long lastTickTime = 0;
    
    private final Set<Currently> activeActivities = EnumSet.noneOf(Currently.class);
    
    private static boolean enabled = false;
    
    public enum Currently {
        FISHING,
        DIANA,
        SPOOKY,
        SHARK
    }
    
    private ActivityMonitor() {
        resetActivityTimer();
    }
    
    public static ActivityMonitor getInstance() {
        if (instance == null) {
            instance = new ActivityMonitor();
        }
        return instance;
    }
    
    public void resetActivityTimer() {
        lastActivityTime = System.currentTimeMillis();
    }
    
    /**
     * Record activity and reset timer
     */
    public void recordActivity(Currently activity) {
        if (!enabled) {
            return;
        }
        
        resetActivityTimer();
        activeActivities.add(activity);
    }
    
    /**
     * Stop tracking a specific activity
     */
    public void stopActivity(Currently activity) {
        activeActivities.remove(activity);
    }
    
    /**
     * Clear all active activities and backup current session data
     */
    public void clearAllActivities() {
        activeActivities.clear();
    }
    
    /**
     * Checks for time since last activity every second and saves cached data if needed
     */
    public void tick() {
        if (!enabled) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastTickTime < TICK_INTERVAL) {
            return;
        }

        lastTickTime = currentTime;
        
        long timeSinceActivity = currentTime - lastActivityTime;
        
        if (timeSinceActivity >= 30000 && hasActiveActivities()) {
            clearAllActivities();
            saveAndClearCaches();            
        }
        
        if (timeSinceActivity >= PAUSE_DELAY_MS) {
            resetActivityTimer();
            setPauseTimers();
        }

        if (currentTime - lastActivityTime >= SAVE_INTERVAL_MS) {
            lastActivityTime = currentTime;
            saveActive();
        }
    }
    
    private void saveAndClearCaches() {
        if (ScStats.isEnabled()) {
            ScStats.getInstance().save();
        }

        DianaStats.getInstance().saveSession();        
    }

    private void saveActive() {
        if (isActive(Currently.FISHING) && ScStats.isEnabled()) {
            ScStats.getInstance().save();
        }
        
        if (isActive(Currently.DIANA) && DianaStats.isEnabled()) {
            DianaStats.getInstance().save();
        }
    }

    private void setPauseTimers() {
        if (DianaStats.isEnabled()) {
            DianaStats.getInstance().setPaused(true);
        }
    }
    
    /**
     * Force save all cached data
     */
    public void forceSave() {
        saveAndClearCaches();
    }
    
    /**
     * Get time since last activity in milliseconds
     */
    public long getTimeSinceLastActivity() {
        return System.currentTimeMillis() - lastActivityTime;
    }
    
    /**
     * Check if a specific activity is currently active
     */
    public boolean isActive(Currently activity) {
        return activeActivities.contains(activity);
    }
    
    /**
     * Check if any activities are currently active
     */
    public boolean hasActiveActivities() {
        return !activeActivities.isEmpty();
    }
    
    /**
     * Get all currently active activities
     */
    public Set<Currently> getActiveActivities() {
        return EnumSet.copyOf(activeActivities);
    }
    
    /**
     * Get the primary activity (first one registered, or null if none)
     */
    public Currently getPrimaryActivity() {
        return activeActivities.isEmpty() ? null : activeActivities.iterator().next();
    }
    
    /**
     * Enable or disable activity monitoring
     */
    public static void refresh() {
        enabled = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.TRACK_SCS, false) ||
                  FishyConfig.getState(me.valkeea.fishyaddons.config.Key.TRACK_DIANA, false);
        ScStats.init();
        DianaStats.refresh();
    }
    
    /**
     * Check if activity monitoring is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }
}