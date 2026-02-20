package me.valkeea.fishyaddons.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.valkeea.fishyaddons.api.skyblock.SlayerTables;
import me.valkeea.fishyaddons.api.skyblock.SlayerTables.SlayerType;
import me.valkeea.fishyaddons.config.StatConfig;
import me.valkeea.fishyaddons.tracker.monitoring.ActivityMonitor;
import me.valkeea.fishyaddons.tracker.monitoring.Currently;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.text.Text;

@SuppressWarnings("squid:S6548")
public class SlayerStats {
    private static final Logger LOGGER = LoggerFactory.getLogger("FishyAddons/SlayerStats");

    private static SlayerStats instance;
    private static boolean enabled = false;

    private boolean paused = false;
    private long pauseStartTime = 0;
    private long sessionStartTime;
    private int sessionBosses = 0;
    private int sessionXp = 0;
    private SlayerType currentType = null;
    private SlayerType pendingType = null;
    private long pendingMessageTime = 0;
    private static final long MESSAGE_TIMEOUT_MS = 5000;

    private static final String TOTAL_BOSSES_KEY = "slayer_%s_total_bosses";
    private static final String TOTAL_XP_KEY = "slayer_%s_total_xp";
    private static final String TOTAL_PLAYTIME_KEY = "slayer_%s_total_playtime_minutes";
    private static final String BU_SESSIONSTART = "backup_slayer_%s_start_seconds";
    private static final String BU_BOSSES = "backup_slayer_%s_bosses";
    private static final String BU_XP = "backup_slayer_%s_xp";
    private static final String BU_CURRENT_TYPE = "backup_slayer_current_type";

    private SlayerStats() {
        validate();
        
        if (!loadBackup()) {
            resetSession();
        }
    }

    public static boolean loaded() {
        return instance != null;
    }

    public static SlayerStats getInstance() {
        if (instance == null && enabled) {
            instance = new SlayerStats();
        }
        return instance;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void refresh() {
        enabled = me.valkeea.fishyaddons.config.FishyConfig.getState(me.valkeea.fishyaddons.config.Key.TRACK_SLAYER, false);
    }

    // --- Internal methods ---

    private void validate() {
        StatConfig.beginBatch();
        
        long now = System.currentTimeMillis() / 1000;
        long maxValid = now + 86400;
        
        for (SlayerType type : SlayerType.values()) {
            long playtime = StatConfig.getSlayer(String.format(TOTAL_PLAYTIME_KEY, type.name()), 0);
            if (playtime < 0) {
                LOGGER.warn("Detected corrupted playtime for {}: {} minutes. Resetting to 0.", type.name(), playtime);
                StatConfig.setSlayer(String.format(TOTAL_PLAYTIME_KEY, type.name()), 0);
            }
            
            if (!isBackupValid(type, maxValid)) {
                clearBackupForType(type);
            }
        }
        
        StatConfig.endBatch();
    }

    private boolean isBackupValid(SlayerType type, long maxValid) {
        long backupSeconds = StatConfig.getSlayer(String.format(BU_SESSIONSTART, type.name()), 0);
        if (backupSeconds <= 0 || backupSeconds > maxValid) {
            if (backupSeconds != 0) {
                LOGGER.warn("Invalid backup timestamp for {}: {} seconds. Clearing backup.", type.name(), backupSeconds);
            }
            return false;
        }
        return true;
    }

    private void clearBackupForType(SlayerType type) {
        StatConfig.setSlayer(String.format(BU_BOSSES, type.name()), 0);
        StatConfig.setSlayer(String.format(BU_SESSIONSTART, type.name()), 0);
        StatConfig.setSlayer(String.format(BU_XP, type.name()), 0);
    }

    private boolean loadBackup() {
        String savedType = StatConfig.getSlayerString(BU_CURRENT_TYPE, "");
        if (savedType.isEmpty()) {
            return false;
        }
        
        SlayerType type;
        try {
            type = SlayerType.valueOf(savedType);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid slayer type in backup: {}", savedType);
            return false;
        }
        
        int buBosses = StatConfig.getSlayer(String.format(BU_BOSSES, type.name()), 0);
        if (buBosses <= 0) {
            return false;
        }
        
        long startTimeSeconds = StatConfig.getSlayer(String.format(BU_SESSIONSTART, type.name()), 0);
        if (!isTimestampValid(startTimeSeconds)) {
            clearBackupForType(type);
            return false;
        }
        
        currentType = type;
        sessionBosses = buBosses;
        sessionStartTime = startTimeSeconds * 1000L;
        sessionXp = StatConfig.getSlayer(String.format(BU_XP, currentType.name()), 0);
        return true;
    }

    private boolean isTimestampValid(long timestampSeconds) {
        return timestampSeconds > 0 && timestampSeconds <= (System.currentTimeMillis() / 1000);
    }

    private void clearBackup() {
        if (currentType == null) return;
        
        StatConfig.beginBatch();
        clearBackupForType(currentType);
        StatConfig.setSlayerString(BU_CURRENT_TYPE, "");
        StatConfig.endBatch();
    }    

    // --- Public methods ---

    /** Pauses or resumes the session timer.*/
    public void setPaused(boolean pause) {
        if (sessionBosses == 0) return;
        if (this.paused == pause) return;
        
        this.paused = pause;
        
        if (pause) {
            pauseStartTime = System.currentTimeMillis();

        } else {
            if (pauseStartTime > 0) {
                long pauseDuration = System.currentTimeMillis() - pauseStartTime;
                sessionStartTime += pauseDuration;
                pauseStartTime = 0;
            }
        }
    }    

    public void saveSession() {
        if (!hasActiveSession()) return;
        
        StatConfig.beginBatch();
        StatConfig.setSlayer(String.format(BU_BOSSES, currentType.name()), sessionBosses);
        StatConfig.setSlayer(String.format(BU_SESSIONSTART, currentType.name()), (int)(sessionStartTime / 1000));
        StatConfig.setSlayer(String.format(BU_XP, currentType.name()), sessionXp);
        StatConfig.setSlayerString(BU_CURRENT_TYPE, currentType.name());
        StatConfig.endBatch();
    }

    private boolean hasActiveSession() {
        return sessionBosses > 0 && currentType != null;
    }

    public void resetSession() {
        sessionStartTime = System.currentTimeMillis();
        sessionBosses = 0;
        sessionXp = 0;
        currentType = null;
        pendingType = null;
        pendingMessageTime = 0;
    }

    public void resetAll() {
        StatConfig.beginBatch();
        
        for (SlayerType type : SlayerType.values()) {
            StatConfig.setSlayer(String.format(TOTAL_BOSSES_KEY, type.name()), 0);
            StatConfig.setSlayer(String.format(TOTAL_XP_KEY, type.name()), 0);
            StatConfig.setSlayer(String.format(TOTAL_PLAYTIME_KEY, type.name()), 0);
        }

        clearBackup();
        resetSession();
        
        StatConfig.endBatch();
    }
    
    public void resetType(SlayerType type) {
        StatConfig.beginBatch();
        
        StatConfig.setSlayer(String.format(TOTAL_BOSSES_KEY, type.name()), 0);
        StatConfig.setSlayer(String.format(TOTAL_XP_KEY, type.name()), 0);
        StatConfig.setSlayer(String.format(TOTAL_PLAYTIME_KEY, type.name()), 0);
        
        if (currentType == type) {
            clearBackup();
            resetSession();
        }
        
        StatConfig.endBatch();
    }

    /** Identify type and mark pending quest completion */
    public static boolean handleSlayerCompletion(String message) {
        SlayerType type = SlayerType.fromString(message);
        if (type != null && message.contains("Slayer LVL")) {
            getInstance().setPendingType(type);
            return true;
        }
        return false;
    }

    private void setPendingType(SlayerType type) {
        pendingType = type;
        pendingMessageTime = System.currentTimeMillis();
    }

    /** Identifies slayer tier from required XP and records completion.*/
    public boolean handleQuestDescription(String message) {
        if (!isPendingTypeValid()) {
            pendingType = null;
            return false;
        }

        int xpRequired = SlayerTables.parseXpFromMessage(message);
        if (xpRequired <= 0) return false;

        int tier = SlayerTables.getTierFromXp(pendingType, xpRequired);
        if (tier <= 0) return false;

        int xpGained = SlayerTables.getXpGained(pendingType, tier);
        recordQuestCompletion(pendingType, tier, xpGained);
        pendingType = null;
        return true;
    }

    private boolean isPendingTypeValid() {
        if (pendingType == null) return false;
        long timeDiff = System.currentTimeMillis() - pendingMessageTime;
        return timeDiff <= MESSAGE_TIMEOUT_MS;
    }

    private void recordQuestCompletion(SlayerType type, int tier, int xp) {

        if (currentType != null && currentType != type) {
            save();
            resetSession();
        }

        if (currentType == null) {
            currentType = type;
            sessionStartTime = System.currentTimeMillis();
        }
        
        if (!isCurrentlyActive()) setPaused(false);

        ActivityMonitor.getInstance().recordActivity(Currently.SLAYER);
        sessionBosses++;
        sessionXp += xp;
        
        sendQuestCompletionMessage(type, tier);
    }

    private void sendQuestCompletionMessage(SlayerType type, int tier) {
        double bossesPerHour = getSessionBossesPerHour();
        double xpPerHour = getSessionXpPerHour();
        
        String message = String.format("§7[§dT%d§7] %s §8| §b%.1f/hr §7(§3%d§7) §8| §5%.0f XP/hr",
            tier,
            type.getCmdName(),
            bossesPerHour,
            sessionBosses,
            xpPerHour
        );
        
        FishyNotis.alert(Text.literal(message));
    }

    public void save() {
        if (!enabled || !hasActiveSession()) return;
        
        StatConfig.beginBatch();
        
        int totalBosses = StatConfig.getSlayer(String.format(TOTAL_BOSSES_KEY, currentType.name()), 0);
        StatConfig.setSlayer(String.format(TOTAL_BOSSES_KEY, currentType.name()), totalBosses + sessionBosses);

        int totalXp = StatConfig.getSlayer(String.format(TOTAL_XP_KEY, currentType.name()), 0);
        StatConfig.setSlayer(String.format(TOTAL_XP_KEY, currentType.name()), totalXp + sessionXp);

        long totalPlaytime = StatConfig.getSlayer(String.format(TOTAL_PLAYTIME_KEY, currentType.name()), 0);
        long sessionMinutes = getSessionDurationMinutes();
        StatConfig.setSlayer(String.format(TOTAL_PLAYTIME_KEY, currentType.name()), (int)(totalPlaytime + sessionMinutes));

        clearBackup();
        StatConfig.endBatch();
    }

    // --- Getters for current session ---
    
    public int getSessionBosses() {
        return sessionBosses;
    }

    public int getSessionXp() {
        return sessionXp;
    }

    public long getSessionDurationMinutes() {
        long duration = System.currentTimeMillis() - sessionStartTime;
        
        if (paused && pauseStartTime > 0) {
            duration -= (System.currentTimeMillis() - pauseStartTime);
        }

        return Math.max(0, duration / 60_000);
    }

    // --- Getters for totals ---
    
    public int getTotalBosses() {
        return getTotalBosses(currentType);
    }
    
    public int getTotalBosses(SlayerType type) {
        if (type == null) return 0;
        int persistent = StatConfig.getSlayer(String.format(TOTAL_BOSSES_KEY, type.name()), 0);
        return persistent + (type == currentType ? sessionBosses : 0);
    }

    public int getTotalXp() {
        return getTotalXp(currentType);
    }
    
    public int getTotalXp(SlayerType type) {
        if (type == null) return 0;
        int persistent = StatConfig.getSlayer(String.format(TOTAL_XP_KEY, type.name()), 0);
        return persistent + (type == currentType ? sessionXp : 0);
    }

    public long getTotalPlaytimeMinutes() {
        return getTotalPlaytimeMinutes(currentType);
    }
    
    public long getTotalPlaytimeMinutes(SlayerType type) {
        if (type == null) { 
            return 0;
        }

        long persistent = StatConfig.getSlayer(String.format(TOTAL_PLAYTIME_KEY, type.name()), 0);
        if (persistent < 0)
            return 0;
        
        return persistent + (type == currentType ? getSessionDurationMinutes() : 0);
    }

    // --- Calculated stats ---
    
    public double getSessionBossesPerHour() {
        return calculateRatePerHour(sessionBosses, getSessionDurationMinutes());
    }

    public double getSessionXpPerHour() {
        return calculateRatePerHour(sessionXp, getSessionDurationMinutes());
    }
    
    private double calculateRatePerHour(double value, long minutes) {
        if (value <= 0) return 0.0;
        long effectiveMinutes = Math.max(1, minutes);
        return (value / effectiveMinutes) * 60.0;
    }

    public double getTotalBossesPerHour() {
        return getTotalBossesPerHour(currentType);
    }
    
    public double getTotalBossesPerHour(SlayerType type) {
        if (type == null) return 0.0;
        return calculateRatePerHour(getTotalBosses(type), getTotalPlaytimeMinutes(type));
    }

    public double getTotalXpPerHour() {
        return getTotalXpPerHour(currentType);
    }
    
    public double getTotalXpPerHour(SlayerType type) {
        if (type == null) return 0.0;
        return calculateRatePerHour(getTotalXp(type), getTotalPlaytimeMinutes(type));
    }

    public double getAverageXpPerBoss() {
        return getAverageXpPerBoss(currentType);
    }
    
    public double getAverageXpPerBoss(SlayerType type) {
        if (type == null) return 0.0;
        int totalBosses = getTotalBosses(type);
        if (totalBosses == 0) return 0.0;
        return getTotalXp(type) / (double)totalBosses;
    }

    // --- Utility ---

    private boolean isCurrentlyActive() {
        return ActivityMonitor.getInstance().isActive(Currently.SLAYER);
    }

    public void sendSlayerStats() {
        sendSlayerStats(currentType);
    }
    
    public void sendSlayerStats(SlayerType type) {
        if (!enabled) {
            return;
        }

        if (type == null) {
            sendAllTypeStats();
            return;
        }
        
        boolean hasSession = (type == currentType && sessionBosses > 0);
        int totalBosses = getTotalBosses(type);
        
        if (!hasSession && totalBosses == 0) {
            FishyNotis.alert(Text.literal("§cNo " + type.getCmdName() + " §cslayer data available!"));
            return;
        }

        FishyNotis.themed2("α ", Text.literal(type.getCmdName() + " Slayer Stats"), " α");
        
        if (hasSession) {
            FishyNotis.alert(Text.literal(String.format("§7Session playtime: §3%dh %dmin", 
                getSessionDurationMinutes() / 60, getSessionDurationMinutes() % 60)));
            FishyNotis.alert(Text.literal(String.format("§7Session: §b%.1f/hr §8(§3%d §7bosses§8)", 
                getSessionBossesPerHour(), sessionBosses)));
            FishyNotis.alert(Text.literal(String.format("§7Session XP: §5%.0f/hr §8(§3%d §7total§8)", 
                getSessionXpPerHour(), sessionXp)));
        }
        
        if (totalBosses > 0) {
            long totalMinutes = getTotalPlaytimeMinutes(type);
            FishyNotis.alert(Text.literal(String.format("§7Total playtime: §3%dh %dmin", 
                totalMinutes / 60, totalMinutes % 60)));
            FishyNotis.alert(Text.literal(String.format("§7Total: §b%.1f/hr §8(§3%d §7bosses§8)", 
                getTotalBossesPerHour(type), totalBosses)));
            FishyNotis.alert(Text.literal(String.format("§7Total XP: §5%.0f/hr §8(§3%d §7total§8)", 
                getTotalXpPerHour(type), getTotalXp(type))));
            FishyNotis.alert(Text.literal(String.format("§7Average XP per boss: §d%.0f", 
                getAverageXpPerBoss(type))));
        }
    }
    
    private void sendAllTypeStats() {
        FishyNotis.themed("α All Slayer Stats α");
        boolean hasAnyData = false;
        
        for (SlayerType type : SlayerType.values()) {
            int totalBosses = getTotalBosses(type);
            if (totalBosses > 0) {
                hasAnyData = true;
                long totalMinutes = getTotalPlaytimeMinutes(type);
                double bossesPerHour = totalMinutes > 0 ? (totalBosses / (double)totalMinutes) * 60.0 : 0;
                FishyNotis.alert(Text.literal(String.format("§3%s§8: §b%.1f/hr §8(§3%d §7bosses, §3%d §7xp§8)",
                    type.getCmdName(), bossesPerHour, totalBosses, getTotalXp(type))));
            }
        }
        
        if (!hasAnyData) {
            FishyNotis.alert(Text.literal("§cNo slayer data available!"));
        } else if (currentType != null && sessionBosses > 0) {
            FishyNotis.alert(Text.literal(String.format("§7Current session: §d%s §8(§3%d §7bosses§8)",
                currentType.getCmdName(), sessionBosses)));
        }
    }
}
