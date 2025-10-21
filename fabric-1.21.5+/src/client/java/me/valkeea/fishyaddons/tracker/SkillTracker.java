package me.valkeea.fishyaddons.tracker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import me.valkeea.fishyaddons.api.skyblock.SkillLevelTables;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.handler.TabScanner;
import me.valkeea.fishyaddons.util.FishyNotis;

public class SkillTracker {
    
    private static class SkillBaseline {
        long xpAbsolute = -1;
        double xpPercentage = -1;
        int baselineLevel = -1;
        boolean isMaxed = false;
        long sessionStartTime = 0;
        
        SkillBaseline() {}
    }
    
    private static boolean enabled = false;
    private boolean downTiming = false;

    private volatile long startTime = System.currentTimeMillis();
    private volatile long pausedTime = 0;
    private volatile long lastXpGain = 0;

    private final AtomicLong totalPausedDuration = new AtomicLong(0);    
    private final AtomicInteger catchCount = new AtomicInteger(0);
    private final AtomicInteger mobCount = new AtomicInteger(0);
    private final AtomicInteger catchRate = new AtomicInteger(-1);
    private final AtomicInteger mobRate = new AtomicInteger(-1);

    // Skill name to total XP gained this session
    private final Map<String, Integer> trackedSkills = new ConcurrentHashMap<>();
    // Skill name to baseline XP
    private final Map<String, SkillBaseline> skillBaselines = new ConcurrentHashMap<>();
    // Skill name to known current level, used for percentage to gained XP conversion
    private final Map<String, Integer> knownSkillLevels = new ConcurrentHashMap<>();
    // Skill name to XP per hour
    private final Map<String, Integer> skillXpPerHour = new ConcurrentHashMap<>();

    private volatile boolean cacheValid = false;
    private static SkillTracker instance = null;
    private SkillTracker() {}
    
    public static synchronized SkillTracker getInstance() {
        if (instance == null) {
            instance = new SkillTracker();
        }
        return instance;
    }

    public static void refresh() {
        enabled = FishyConfig.getState(me.valkeea.fishyaddons.config.Key.HUD_SKILL_XP_ENABLED, false);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public boolean isDownTiming() {
        return downTiming;
    }

    public void onCatch(boolean wasDh) {
        if (!enabled) return;
        catchCount.incrementAndGet();
        mobCount.addAndGet(wasDh ? 2 : 1);
    }

    /**
     * Track skill progress by comparing current XP to baseline.
     */
    public void onXpGain(String skillName, String progressInfo) {
        if (skillName == null || skillName.trim().isEmpty() || progressInfo == null) {
            return;
        }
        
        var baseline = skillBaselines.computeIfAbsent(skillName, k -> new SkillBaseline());
        Integer currentLevel = knownSkillLevels.get(skillName);
        
        if (baseline.sessionStartTime == 0) {
            if (currentLevel == null && TabScanner.scanForSkills()) {
                currentLevel = knownSkillLevels.get(skillName);
            }
            setupBaseline(skillName, baseline, progressInfo, currentLevel);
            return;
        }
        
        long currentXp = calcTotalXp(skillName, progressInfo, currentLevel);
        long baselineXp = calcBaselineTotalXp(skillName, baseline);
        
        
        if (currentXp > 0 && baselineXp > 0) {
            long totalXpGained = currentXp - baselineXp;

            if (totalXpGained > 0) {
                trackedSkills.put(skillName, (int) totalXpGained);
                
                checkDt();
                
                if (startTime == 0) {
                    startTime = System.currentTimeMillis();
                }
                
                cacheValid = false;
                lastXpGain = System.currentTimeMillis();
            }
        }
    }

    private void checkDt() {
        if (downTiming) {
            toggleDownTime();
        } else if (pausedTime > 0) {
            totalPausedDuration.addAndGet(System.currentTimeMillis() - pausedTime);
            pausedTime = 0;
        }
    }
    
    private void setupBaseline(String skillName, SkillBaseline baseline, String progressInfo, Integer level) {
        baseline.sessionStartTime = System.currentTimeMillis();
        
        if (progressInfo.contains("/")) {
            String[] parts = progressInfo.split("/");

            if (parts.length >= 1) {
                try {
                    baseline.xpAbsolute = Long.parseLong(parts[0].replace(",", ""));
                    baseline.isMaxed = true;

                } catch (NumberFormatException e) {
                    System.err.println("[FishyAddons] Error parsing baseline absolute XP: " + parts[0]);
                }
            }

        } else if (progressInfo.contains("%") && level != null) {
            try {
                baseline.xpPercentage = Double.parseDouble(progressInfo.replace("%", ""));
                baseline.baselineLevel = level;
                baseline.isMaxed = false;

            } catch (NumberFormatException e) {
                System.err.println("[FishyAddons] Error parsing baseline percentage: " + progressInfo);
            }

        } else {
            System.out.println("[FishyAddons] Could not set baseline for " + skillName + " - progressInfo: '" + progressInfo + "', level: " + level);
        }
    }
    
    private long calcTotalXp(String skillName, String progressInfo, Integer level) {
        if (progressInfo.contains("/")) {
            String[] parts = progressInfo.split("/");

            if (parts.length >= 1) {
                try {
                    return Long.parseLong(parts[0].replace(",", ""));
                } catch (NumberFormatException e) {
                    System.err.println("[FishyAddons] Error parsing current absolute XP: " + parts[0]);
                }
            }

        } else if (progressInfo.contains("%") && level != null) {
            try {
                double percentage = Double.parseDouble(progressInfo.replace("%", ""));
                return SkillLevelTables.calculateCurrentXp(skillName, level, percentage);

            } catch (NumberFormatException e) {
                System.err.println("[FishyAddons] Error parsing current percentage: " + progressInfo);
            }

        } else {
            System.out.println("[FishyAddons] Cannot calculate current XP - missing level or invalid format");
        }

        return -1;
    }

    private long calcBaselineTotalXp(String skillName, SkillBaseline baseline) {
        if (baseline.isMaxed && baseline.xpAbsolute >= 0) {
            return baseline.xpAbsolute;
        } else if (!baseline.isMaxed && baseline.baselineLevel >= 0 && baseline.xpPercentage >= 0) {
            return SkillLevelTables.calculateCurrentXp(skillName, baseline.baselineLevel, baseline.xpPercentage);
        }
        return -1;
    }

    /**
     * Update skill level from tab scanning
     */
    public void updateSkillLevel(String skillName, int level) {
        knownSkillLevels.put(skillName, level);
    }

    // Check if no xp gain for 1.5 minutes, if so, pause tracking
    public void tick() {
        if (!enabled) return;
        if (lastXpGain > 0 && System.currentTimeMillis() - lastXpGain > 90_000) {
            pauseTracking();
        }
    }

    public void pauseTracking() {
        if (pausedTime == 0 && !downTiming) { // Don't set pausedTime if already in downtime
            pausedTime = System.currentTimeMillis();
        }

        // Only clear data on inactivity if not in manual downtime mode
        if (pausedTime > 0 && System.currentTimeMillis() - pausedTime > 900_000 && !downTiming) {
            trackedSkills.clear();
            skillBaselines.clear();
            knownSkillLevels.clear();
            totalPausedDuration.set(0);
            startTime = System.currentTimeMillis();
            pausedTime = 0;
            lastXpGain = 0;
            catchCount.set(0);
            mobCount.set(0);
            cacheValid = false;
            FishyNotis.send("Skill tracking data cleared due to extended inactivity.");
        }
    }

    public void toggleDownTime() {
        downTiming = !downTiming;
        if (downTiming) {
            if (pausedTime == 0) {
                pausedTime = System.currentTimeMillis();
            }
            FishyNotis.warn("You are now downtiming! Skill XP tracking has been paused.");
        } else {
            if (pausedTime > 0) {
                totalPausedDuration.addAndGet(System.currentTimeMillis() - pausedTime);
                pausedTime = 0;
            }
            lastXpGain = 0;
            FishyNotis.themed("§oYou are no longer downtiming! Skill XP tracking has been resumed.");
        }
    }

    public boolean isPaused() {
        return pausedTime > 0;
    }

    public int getCatchCount() {
        return catchCount.get();
    }

    public int getMobCount() {
        return mobCount.get();
    }

    public int getCatchRate() {
        if (!cacheValid) {
            updateCache();
        }
        return catchRate.get();
    }

    public int getMobRate() {
        if (!cacheValid) {
            updateCache();
        }
        return mobRate.get();
    }    

    public int getTotalXp(String skillName) {
        if (skillName == null || skillName.trim().isEmpty()) {
            return 0;
        }
        return trackedSkills.getOrDefault(skillName, 0);
    }

    public int getXpPerHour(String skillName) {
        if (!cacheValid) {
            updateCache();
        }
        return skillXpPerHour.getOrDefault(skillName, 0);
    }
    
    private void updateCache() {
        if (cacheValid) return;
        
        long currentTime = System.currentTimeMillis();
        long effectiveDuration = currentTime - startTime - totalPausedDuration.get();

        if (effectiveDuration <= 0) {
            skillXpPerHour.clear();
            mobRate.set(-1);
            catchRate.set(-1);

        } else {
            double hours = effectiveDuration / 3600000.0;
            
            skillXpPerHour.clear();
            for (java.util.Map.Entry<String, Integer> entry : trackedSkills.entrySet()) {
                int skillRate = (int) Math.round(entry.getValue() / hours);
                skillXpPerHour.put(entry.getKey(), skillRate);
            }

            mobRate.set((int) Math.round(mobCount.get() / hours));
            catchRate.set((int) Math.round(catchCount.get() / hours));
        }
        
        cacheValid = true;
    }

    public boolean hasNewData() {
        return cacheValid;
    }    
    
    public java.util.Set<String> getTrackedSkills() {
        return new java.util.HashSet<>(trackedSkills.keySet());
    }

    public String getTrackedSkill() {
        if (trackedSkills.size() == 1) {
            return trackedSkills.keySet().iterator().next();
        }
        return null;
    }
    
    public int getSkillXp(String skillName) {
        return trackedSkills.getOrDefault(skillName, 0);
    }
    
    public boolean hasMultipleSkills() {
        return trackedSkills.size() > 1;
    }
}