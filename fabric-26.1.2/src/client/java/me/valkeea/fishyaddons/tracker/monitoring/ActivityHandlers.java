package me.valkeea.fishyaddons.tracker.monitoring;

import me.valkeea.fishyaddons.tracker.DianaStats;
import me.valkeea.fishyaddons.tracker.SlayerStats;
import me.valkeea.fishyaddons.tracker.fishing.ScStats;

public class ActivityHandlers {
    
    public static final ActivityHandler FISHING = new ActivityHandler() {
        @Override
        public void save() {
            if (ScStats.isEnabled()) {
                ScStats.getInstance().save();
            }
        }
        
        @Override
        public void setPaused(boolean paused) {
            // Fishing doesn't have a pause mechanism
        }
        
        @Override
        public boolean isEnabled() {
            return ScStats.isEnabled();
        }
    };
    
    public static final ActivityHandler DIANA = new ActivityHandler() {
        @Override
        public void save() {
            if (DianaStats.isEnabled()) {
                DianaStats.getInstance().save();
            }
        }
        
        @Override
        public void saveSession() {
            if (DianaStats.isEnabled()) {
                DianaStats.getInstance().saveSession();
            }
        }
        
        @Override
        public void setPaused(boolean paused) {
            if (DianaStats.isEnabled()) {
                DianaStats.getInstance().setPaused(paused);
                DianaStats.getInstance().saveSession();
            }
        }
        
        @Override
        public boolean isEnabled() {
            return DianaStats.isEnabled();
        }
    };
    
    public static final ActivityHandler SLAYER = new ActivityHandler() {
        @Override
        public void save() {
            if (SlayerStats.isEnabled()) {
                SlayerStats.getInstance().save();
            }
        }
        
        @Override
        public void saveSession() {
            if (SlayerStats.isEnabled()) {
                SlayerStats.getInstance().saveSession();
            }
        }
        
        @Override
        public void setPaused(boolean paused) {
            if (SlayerStats.isEnabled()) {
                SlayerStats.getInstance().setPaused(paused);
                SlayerStats.getInstance().saveSession();
            }
        }
        
        @Override
        public boolean isEnabled() {
            return SlayerStats.isEnabled();
        }
    };
    
    private ActivityHandlers() {
        // Utility class
    }
}
