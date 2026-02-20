package me.valkeea.fishyaddons.tracker.monitoring;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.tracker.DianaStats;
import me.valkeea.fishyaddons.tracker.SlayerStats;
import me.valkeea.fishyaddons.tracker.fishing.ScStats;

public class ActivityMonitor {
    private static ActivityMonitor instance = null;

    private static final long CLEAR_DELAY_MS = 30000;
    private static final long SAVE_INTERVAL_MS = 300000;
    private static final long TICK_INTERVAL = 1000;
    
    private long lastActivityTime = 0;
    private long lastTickTime = 0;
    private long lastSaveTime = 0;
    
    private final Set<Currently> activeActivities = EnumSet.noneOf(Currently.class);
    private final Map<Currently, Long> activityTimers = new EnumMap<>(Currently.class);
    private final Map<Currently, PausebleActivity> registeredActivities = new ConcurrentHashMap<>();
    
    private static boolean enabled = false;
    
    private ActivityMonitor() {
        resetActivityTimer();
        registerDefaultActivities();
    }
    
    public static ActivityMonitor getInstance() {
        if (instance == null) {
            instance = new ActivityMonitor();
        }
        return instance;
    }
    
    private void registerDefaultActivities() {

        registerActivity(new PausebleActivity.Builder(Currently.FISHING)
            .handler(ActivityHandlers.FISHING)
            .enableAutoSave()
            .build());
        
        registerActivity(new PausebleActivity.Builder(Currently.DIANA)
            .handler(ActivityHandlers.DIANA)
            .pauseDelay(180000)
            .enableAutoPause()
            .enableAutoSave()
            .build());
        
        registerActivity(new PausebleActivity.Builder(Currently.SLAYER)
            .handler(ActivityHandlers.SLAYER)
            .pauseDelay(180000)
            .enableAutoPause()
            .enableAutoSave()
            .build());
        
        registerActivity(new PausebleActivity.Builder(Currently.SPOOKY).build());
        registerActivity(new PausebleActivity.Builder(Currently.SHARK).build());
    }
    
    /**
     * Register an activity with its configuration
     */
    public void registerActivity(PausebleActivity config) {
        registeredActivities.put(config.getType(), config);
    }
    
    /**
     * Unregister an activity
     */
    public void unregisterActivity(Currently type) {
        registeredActivities.remove(type);
        activeActivities.remove(type);
        activityTimers.remove(type);
    }
    
    /**
     * Get the configuration for a registered activity
     */
    public PausebleActivity getActivityConfig(Currently type) {
        return registeredActivities.get(type);
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
        activityTimers.put(activity, System.currentTimeMillis());
        
        // Also track parent activity if this is a subactivity
        activity.getParent().ifPresent(parent -> {
            activeActivities.add(parent);
            activityTimers.put(parent, System.currentTimeMillis());
        });
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
        
        if (timeSinceActivity >= CLEAR_DELAY_MS && hasActiveActivities()) {
            clearAllActivities();
            saveAllActivities();            
        }

        pauseInactiveActivities(currentTime);

        if (currentTime - lastSaveTime >= SAVE_INTERVAL_MS) {
            lastSaveTime = currentTime;
            saveActiveActivities();
        }
    }
    
    /**
     * Pause activities that have been inactive for their configured pause delay
     */
    private void pauseInactiveActivities(long currentTime) {
        for (Map.Entry<Currently, Long> entry : activityTimers.entrySet()) {
            Currently type = entry.getKey();
            Long lastRecorded = entry.getValue();
            
            PausebleActivity config = registeredActivities.get(type);
            if (config == null || !config.isAutoPauseEnabled()) {
                continue;
            }
            
            long timeSinceActivityRecorded = currentTime - lastRecorded;
            if (timeSinceActivityRecorded >= config.getPauseDelayMs()) {
                config.getHandler().ifPresent(handler -> {
                    if (handler.isEnabled()) {
                        handler.setPaused(true);
                    }
                });
                activityTimers.remove(type);
            }
        }
    }
    
    /**
     * Save all registered activities (called on clear)
     */
    private void saveAllActivities() {
        activeActivities.forEach(type -> {
            PausebleActivity config = registeredActivities.get(type);
            if (config != null && config.isAutoSaveEnabled()) {
                config.getHandler().ifPresent(handler -> {
                    if (handler.isEnabled()) {
                        handler.save();
                    }
                });
            }
        });
    }

    /**
     * Save currently active activities
     */
    private void saveActiveActivities() {
        for (Currently type : activeActivities) {
            PausebleActivity config = registeredActivities.get(type);
            if (config != null && config.isAutoSaveEnabled()) {
                config.getHandler().ifPresent(handler -> {
                    if (handler.isEnabled()) {
                        handler.save();
                    }
                });
            }
        }
    }
    
    /**
     * Force save all cached data
     */
    public void forceSave() {
        saveAllActivities();
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
                  FishyConfig.getState(me.valkeea.fishyaddons.config.Key.TRACK_DIANA, false) ||
                  FishyConfig.getState(me.valkeea.fishyaddons.config.Key.TRACK_SLAYER, false);

        ScStats.init();
        DianaStats.refresh();
        SlayerStats.refresh();
    }
    
    /**
     * Check if activity monitoring is enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }
}
