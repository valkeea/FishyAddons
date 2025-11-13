package me.valkeea.fishyaddons.tracker;

import me.valkeea.fishyaddons.config.StatConfig;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.text.Text;

@SuppressWarnings("squid:S6548")
public class DianaStats {
    private static DianaStats instance;
    private static boolean enabled = false;

    private boolean paused = false;
    private boolean recentLs = false;
    private long pauseStartTime = 0;        
    private long sessionStartTime;
    private double sessionBurrows = 0;
    private int sessionMobs = 0;
    private double sinceInq = 0;
    private double mobSinceInq = 0;

    private static final String TOTAL_BURROWS_KEY = "diana_total_burrows";
    private static final String TOTAL_MOBS_KEY = "diana_total_mobs";
    private static final String TOTAL_PLAYTIME_KEY = "diana_total_playtime_minutes";
    private static final String TOTAL_INQ_KEY = "diana_total_inquisitors";
    private static final String SINCE_INQ_KEY = "diana_since_inquisitor";
    private static final String MOB_SINCE_INQ_KEY = "diana_mob_since_inquisitor";
    private static final String OWN_CHIMERA_KEY = "diana_total_own_chimeras";
    private static final String LS_CHIM_KEY = "diana_total_ls_chimeras";
    private static final String INQ_SINCE_CHIM = "diana_inquisitor_since_chimera";

    private static final String BU_SESSIONSTART = "backup_diana_start";
    private static final String BU_BURROWS = "backup_diana_burrows";
    private static final String BU_MOBS = "backup_diana_mobs";
    private static final String BU_SINCE_INQ = "backup_diana_since_inq";
    private static final String BU_MOB_SINCE_INQ = "backup_diana_mob_since_inq";

    private static final String[] MOB_PREFIXES = {"oi!", "uh oh!", "yikes!", "woah!", "oh!" };

    private DianaStats() {
        if (!loadBackup()) {
            resetSession();
        }
    }

    public static boolean loaded() {
        return instance != null;
    }

    public static DianaStats getInstance() {
        if (instance == null) {
            instance = new DianaStats();
        }
        return instance;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void refresh() {
        enabled = me.valkeea.fishyaddons.config.FishyConfig.getState(me.valkeea.fishyaddons.config.Key.TRACK_DIANA, false);
    }

    public void setPaused(boolean pause) {
        if (sessionBurrows == 0) {
            return;
        }
        var wasPaused = this.paused;
        if (this.paused != pause) {
            this.paused = pause;
            if (pause) {
                pauseStartTime = System.currentTimeMillis();
            } else {
                ActivityMonitor.getInstance().resetActivityTimer();
                if (wasPaused) {
                    long pausedDuration = System.currentTimeMillis() - pauseStartTime;
                    sessionStartTime += pausedDuration;
                }
            }
        }
    }

    /*
     * Load session data from backup storage if available
     */
    private boolean loadBackup() {
        int buBurrows = StatConfig.getDiana(BU_BURROWS, 0);
        if (buBurrows == 0) {
            return false;
        }
        sessionBurrows = buBurrows;
        sessionStartTime = StatConfig.getDiana(BU_SESSIONSTART, (int)System.currentTimeMillis());
        sessionMobs = StatConfig.getDiana(BU_MOBS, 0);
        sinceInq = StatConfig.getDiana(BU_SINCE_INQ, 0);
        mobSinceInq = StatConfig.getDiana(BU_MOB_SINCE_INQ, 0);
        return true;
    }

    public void saveSession() {
        if (sessionBurrows == 0) {
            return;
        }
        
        StatConfig.setDiana(BU_BURROWS, (int)sessionBurrows);
        StatConfig.setDiana(BU_SESSIONSTART, (int)sessionStartTime);
        StatConfig.setDiana(BU_MOBS, sessionMobs);
        StatConfig.setDiana(BU_SINCE_INQ, (int)sinceInq);
        StatConfig.setDiana(BU_MOB_SINCE_INQ, (int)mobSinceInq);
    }

    private void clearBackup() {
        StatConfig.setDiana(BU_BURROWS, 0);
        StatConfig.setDiana(BU_SESSIONSTART, 0);
        StatConfig.setDiana(BU_MOBS, 0);
        StatConfig.setDiana(BU_SINCE_INQ, 0);
        StatConfig.setDiana(BU_MOB_SINCE_INQ, 0);
    }


    /**
     * Reset session stats (called on new session or manual reset)
     */
    public void resetSession() {
        sessionStartTime = System.currentTimeMillis();
        sessionBurrows = 0;
        sessionMobs = 0;
        loadSinceInq();
        loadMobSinceInq();
    }

    public void resetAll() {
        StatConfig.setDiana(TOTAL_BURROWS_KEY, 0);
        StatConfig.setDiana(TOTAL_MOBS_KEY, 0);
        StatConfig.setDiana(TOTAL_PLAYTIME_KEY, 0);
        StatConfig.setDiana(TOTAL_INQ_KEY, 0);
        StatConfig.setDiana(SINCE_INQ_KEY, 0);
        StatConfig.setDiana(MOB_SINCE_INQ_KEY, 0);
        StatConfig.setDiana(OWN_CHIMERA_KEY, 0);
        StatConfig.setDiana(LS_CHIM_KEY, 0);
        StatConfig.setDiana(INQ_SINCE_CHIM, 0);
        clearBackup();
        resetSession();
    }    

    public boolean handleChat(String s) {
        if (s.startsWith("you dug out a griffin burrow!") || s.contains("you finished the griffin burrow chain!")) {
            burrows();
            return true;
        }

        if (s.contains("you dug out a minos inquisitor!")) {
            inq();
            mob(true);
            return true;
        }

        for (String prefix : MOB_PREFIXES) {
            if (s.startsWith(prefix)) {
                mob(false);
                return true;
            }
        }

        if (s.contains("chimera") && (s.startsWith("rare drop!"))) {
            chim(recentLs);
        }

        recentLs = s.startsWith("loot share");
        return false;
    }

    private void burrows() {
        if (!isCurrentlyActive()) {
            setPaused(false);
        }
        
        ActivityMonitor.getInstance().recordActivity(ActivityMonitor.Currently.DIANA);
        sessionBurrows++;
        sinceInq++;
    }

    private void inq() {
        ActivityMonitor.getInstance().recordActivity(ActivityMonitor.Currently.DIANA);
        
        int totalInq = StatConfig.getDiana(TOTAL_INQ_KEY, 0);
        StatConfig.setDiana(TOTAL_INQ_KEY, totalInq + 1);

        sinceInq = 0;
        StatConfig.setDiana(SINCE_INQ_KEY, 0);

        int inqSinceChim = StatConfig.getDiana(INQ_SINCE_CHIM, 0);
        StatConfig.setDiana(INQ_SINCE_CHIM, inqSinceChim + 1);
    }

    private void mob(boolean wasInq) {
        sessionMobs++;

        if (wasInq) {
            mobSinceInq = 0;
        } else {
            mobSinceInq++;
        }
    }

    private void chim(boolean wasLs) {
        ActivityMonitor.getInstance().recordActivity(ActivityMonitor.Currently.DIANA);

        if (!wasLs) {
            StatConfig.setDiana(INQ_SINCE_CHIM, 0);
            int ownChimera = StatConfig.getDiana(OWN_CHIMERA_KEY, 0);
            StatConfig.setDiana(OWN_CHIMERA_KEY, ownChimera + 1);
        } else {
            int totalLsChim = StatConfig.getDiana(LS_CHIM_KEY, 0);
            StatConfig.setDiana(LS_CHIM_KEY, totalLsChim + 1);
        }
    }

    public void save() {
        if (!enabled || sessionBurrows == 0) return;
        
        double totalBurrows = StatConfig.getDiana(TOTAL_BURROWS_KEY, 0);
        StatConfig.setDiana(TOTAL_BURROWS_KEY, (int)(totalBurrows + sessionBurrows));

        long totalPlaytime = StatConfig.getDiana(TOTAL_PLAYTIME_KEY, 0);
        StatConfig.setDiana(TOTAL_PLAYTIME_KEY, (int)(totalPlaytime + getSessionDurationMinutes()));

        StatConfig.setDiana(SINCE_INQ_KEY, (int)sinceInq);

        double totalMobs = StatConfig.getDiana(TOTAL_MOBS_KEY, 0);
        StatConfig.setDiana(TOTAL_MOBS_KEY, (int)(totalMobs + sessionMobs));

        StatConfig.setDiana(MOB_SINCE_INQ_KEY, (int)mobSinceInq);
        clearBackup();    
    }

    // --- Getters for current session ---
    
    public double getSessionBurrows() {
        return sessionBurrows;
    }

    public long getSessionDurationMinutes() {
        return Math.max(0, System.currentTimeMillis() - sessionStartTime) / (60 * 1000);
    }

    public double getSinceInq() {
        return sinceInq;
    }

    public int getSessionMobs() {
        return sessionMobs;
    }

    public double getMobSinceInq() {
        return mobSinceInq;
    }

    // --- Getters for totals (session + persistent) ---
    
    public double getTotalBurrows() {
        return StatConfig.getDiana(TOTAL_BURROWS_KEY, 0) + sessionBurrows;
    }

    public long getTotalPlaytimeMinutes() {
        return StatConfig.getDiana(TOTAL_PLAYTIME_KEY, 0) + getSessionDurationMinutes();
    }

    public int getTotalInqCount() {
        return StatConfig.getDiana(TOTAL_INQ_KEY, 0);
    }

    public int getTotalChimeraCount() {
        int ownChimera = StatConfig.getDiana(OWN_CHIMERA_KEY, 0);
        int lsChimera = StatConfig.getDiana(LS_CHIM_KEY, 0);
        return ownChimera + lsChimera;
    }

    public int getOwnChimeraCount() {
        return StatConfig.getDiana(OWN_CHIMERA_KEY, 0);
    }

    public int getLsChimeraCount() {
        return StatConfig.getDiana(LS_CHIM_KEY, 0);
    }

    public double getTotalMobs() {
        return StatConfig.getDiana(TOTAL_MOBS_KEY, 0) + (double)sessionMobs;
    }

    // --- Calculated stats for commands ---
    
    public double getSessionBurrowsPerHour() {
        long sessionMinutes = getSessionDurationMinutes();
        if (sessionMinutes == 0) return 0;
        return (sessionBurrows / sessionMinutes) * 60.0;
    }

    public double getTotalBurrowsPerHour() {
        long totalMinutes = getTotalPlaytimeMinutes();
        if (totalMinutes == 0) return 0;
        return (getTotalBurrows() / totalMinutes) * 60.0;
    }

    public double getInqChancePercent() {
        double totalBurrows = getTotalBurrows();
        if (totalBurrows == 0) return 0;
        return (getTotalInqCount() / totalBurrows) * 100.0;
    }

    public double getAvgBurrowsPerInq() {
        int totalInq = getTotalInqCount();
        if (totalInq == 0) return 0;
        return getTotalBurrows() / totalInq;
    }

    public double getAvgMobsPerInq() {
        int totalInq = getTotalInqCount();
        if (totalInq == 0) return 0;
        return getTotalMobs() / totalInq;
    }

    public double getMobInqChancePercent() {
        double totalMobs = getTotalMobs();
        if (totalMobs == 0) return 0;
        return (getTotalInqCount() / totalMobs) * 100.0;
    }

    // --- Load ---
    
    private void loadSinceInq() {
        sinceInq = StatConfig.getDiana(SINCE_INQ_KEY, 0);
    }

    private void loadMobSinceInq() {
        mobSinceInq = StatConfig.getDiana(MOB_SINCE_INQ_KEY, 0);
    }

    /**
     * Check if player is currently dianaing
     */
    private boolean isCurrentlyActive() {
        return ActivityMonitor.getInstance().isActive(ActivityMonitor.Currently.DIANA);
    }

    public void sendDianaStats() {
        if (!enabled) {
            return;
        }

        double totalBurrows = getTotalBurrows();
        double burrowsPerHour = getSessionBurrowsPerHour();
        if (sessionBurrows > 0) {
            FishyNotis.themed("α Diana Stats α");
            FishyNotis.alert(Text.literal(String.format("§7Session playtime: §3%dh %dmin", getSessionDurationMinutes() / 60, getSessionDurationMinutes() % 60)));
            FishyNotis.alert(Text.literal(String.format("§7Total playtime: §3%dh %dmin", getTotalPlaytimeMinutes() / 60, getTotalPlaytimeMinutes() % 60)));
            FishyNotis.alert(Text.literal(String.format("§7Session Bph: §d%.1f", burrowsPerHour) + String.format(" §8 (§b%.1f§8)", sessionBurrows)));
            FishyNotis.alert(Text.literal(String.format("§7Total Bph: §d%.1f", getTotalBurrowsPerHour()) + String.format(" §8 (§b%.1f§8)", totalBurrows)));
            FishyNotis.alert(Text.literal(String.format("§7Burrows since Inq: §d%d", (int) getSinceInq())));
            FishyNotis.alert(Text.literal(String.format("§7Burrows/Inq: §d%.1f", getTotalBurrows() / Math.max(1, getTotalInqCount()))));
            FishyNotis.alert(Text.literal(String.format("§7Mobs since Inq: §d%d", (int) getMobSinceInq())));
            FishyNotis.alert(Text.literal(String.format("§7Mobs/Inq: §d%.1f", getTotalMobs() / Math.max(1, getTotalInqCount()))));
            FishyNotis.alert(Text.literal(String.format("§7Inq chance §8(§7burrows§8)§7: §d%.2f%%", getTotalInqCount() / Math.max(1, totalBurrows) * 100.0) + String.format(" §8 (§d%d", getTotalInqCount()) + "§8)"));
            FishyNotis.alert(Text.literal(String.format("§7Inq chance §8(§7mobs§8)§7: §d%.2f%%", getMobInqChancePercent()) + String.format(" §8 (§b%d", getTotalInqCount()) + "§8)"));
            FishyNotis.alert(Text.literal(String.format("§7Inq since Chim: §d%d", StatConfig.getDiana(INQ_SINCE_CHIM, 0))));
            FishyNotis.alert(Text.literal("§7Chimera chance: §d" + String.format("%.2f%%", ((double) getOwnChimeraCount() / Math.max(1, getTotalInqCount())) * 100.0) + " §8 (" + String.format("§b%d", getOwnChimeraCount()) +
            String.format("§b + %d", getLsChimeraCount()) + " §bls§8)"));
            me.valkeea.fishyaddons.command.TrackerCmd.profitPerHour();
        } else {
            FishyNotis.send("No diana activity this session.");
        }
    }
}
