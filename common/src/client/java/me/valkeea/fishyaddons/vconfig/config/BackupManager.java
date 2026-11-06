package me.valkeea.fishyaddons.vconfig.config;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import me.valkeea.fishyaddons.vconfig.api.Config;

/**
 * Lists and restores dated backup folders created by {@link BaseConfig}.
 */
public final class BackupManager {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int STAMP_LENGTH = 15;

    private BackupManager() {}

    /**
     * @param dir backup folder
     * @param label readable description of the backup
     * @param files config file names contained in the backup
     */
    public record Backup(File dir, String label, List<String> files) {}

    /** All known backups, newest first. */
    public static List<Backup> list() {
        var configs = Config.getConfigs();
        List<Backup> result = new ArrayList<>();
        if (configs.isEmpty()) return result;

        File backupDir = configs.values().iterator().next().getBackupDir();
        File[] dirs = backupDir.listFiles(File::isDirectory);
        if (dirs == null) return result;

        List<File> sorted = new ArrayList<>(List.of(dirs));
        sorted.sort(Comparator.comparing(File::getName).reversed());

        for (File dir : sorted) {
            if (isMigrationFolder(dir.getName())) continue;
            List<String> files = new ArrayList<>();
            for (BaseConfig bc : configs.values()) {
                if (bc != null && new File(dir, bc.getConfigName()).isFile()) {
                    files.add(bc.getConfigName());
                }
            }
            if (!files.isEmpty()) {
                result.add(new Backup(dir, describe(dir.getName()), files));
            }
        }
        return result;
    }

    /**
     * Restore every config file present in the backup.
     * @return the names of the files that were restored
     */
    public static List<String> restore(Backup backup) {
        List<String> restored = new ArrayList<>();
        for (BaseConfig bc : Config.getConfigs().values()) {
            if (bc == null || !backup.files().contains(bc.getConfigName())) continue;
            if (bc.restoreFromFile(new File(backup.dir(), bc.getConfigName()))) {
                restored.add(bc.getConfigName());
            }
        }
        return restored;
    }

    private static String describe(String folderName) {
        if (folderName.length() >= STAMP_LENGTH) {
            try {
                var time = LocalDateTime.parse(folderName.substring(folderName.length() - STAMP_LENGTH), STAMP);
                return "Backup" + " " + DISPLAY.format(time);
            } catch (DateTimeParseException _) {
                // Fall to raw name
            }
        }
        return folderName;
    }

    private static boolean isMigrationFolder(String folderName) {
        return folderName.startsWith("migration_");
    }
}
