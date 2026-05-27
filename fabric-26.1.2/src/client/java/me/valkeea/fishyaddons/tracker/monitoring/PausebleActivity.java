package me.valkeea.fishyaddons.tracker.monitoring;

import java.util.Optional;

/**
 * Configuration for an activity including pause and save timing.
 */
public class PausebleActivity {
    
    public static final long DEFAULT_PAUSE_DELAY_MS = 180000;
    public static final long DEFAULT_CLEAR_DELAY_MS = 30000;
    
    private final Currently type;
    private final ActivityHandler handler;
    private final long pauseDelayMs;
    private final boolean enableAutoPause;
    private final boolean enableAutoSave;
    
    private PausebleActivity(Builder builder) {
        this.type = builder.type;
        this.handler = builder.handler;
        this.pauseDelayMs = builder.pauseDelayMs;
        this.enableAutoPause = builder.enableAutoPause;
        this.enableAutoSave = builder.enableAutoSave;
    }
    
    public Currently getType() {
        return type;
    }
    
    public Optional<ActivityHandler> getHandler() {
        return Optional.ofNullable(handler);
    }
    
    public long getPauseDelayMs() {
        return pauseDelayMs;
    }
    
    public boolean isAutoPauseEnabled() {
        return enableAutoPause && handler != null;
    }
    
    public boolean isAutoSaveEnabled() {
        return enableAutoSave && handler != null;
    }

    public static class Builder {
        private final Currently type;
        private ActivityHandler handler;
        private long pauseDelayMs = DEFAULT_PAUSE_DELAY_MS;
        private boolean enableAutoPause = false;
        private boolean enableAutoSave = false;
        
        public Builder(Currently type) {
            this.type = type;
        }
        
        /**
         * Set the handler for this activity
         */
        public Builder handler(ActivityHandler handler) {
            this.handler = handler;
            return this;
        }
        
        /**
         * Set the pause delay in milliseconds
         */
        public Builder pauseDelay(long pauseDelayMs) {
            this.pauseDelayMs = pauseDelayMs;
            return this;
        }
        
        /**
         * Enable automatic pausing when activity is inactive
         */
        public Builder enableAutoPause() {
            this.enableAutoPause = true;
            return this;
        }
        
        /**
         * Enable automatic saving during active sessions
         */
        public Builder enableAutoSave() {
            this.enableAutoSave = true;
            return this;
        }
        
        public PausebleActivity build() {
            return new PausebleActivity(this);
        }
    }
}
