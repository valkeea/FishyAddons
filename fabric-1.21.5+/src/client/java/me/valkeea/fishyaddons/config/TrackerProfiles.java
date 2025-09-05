package me.valkeea.fishyaddons.config;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import me.valkeea.fishyaddons.tracker.ItemTrackerData;
import me.valkeea.fishyaddons.util.FishyNotis;
import net.minecraft.text.Text;

public class TrackerProfiles {  
    private static final String TRACKER_BASE_PATH = "config/fishyaddons/tracker/";
    private static final String BACKUP_DIR = "config/fishyaddons/backup/tracker/";
    private static final String TRACKER_BASE = "profittracker_";
    private static final String JSON_EXTENSION = ".json";
    private static final String PROFILE_NAME_PATTERN = "[^a-z0-9_-]";
    
    public static void backupAll() {
        try {
            Path backupDir = Paths.get(BACKUP_DIR);
            Files.createDirectories(backupDir);
            for (String profile : getAvailableProfiles()) {
                Path src = Paths.get(getTrackerFilePath(profile));
                if (Files.exists(src)) {
                    String backupName = TRACKER_BASE + profile + JSON_EXTENSION;
                    Path backupFile = backupDir.resolve(backupName);
                    Files.copy(src, backupFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (Exception e) {
            System.err.println("Error saving tracker profile backups: " + e.getMessage());
        }
    }

    public static boolean restoreBackup() {
        try {
            Path backupFile = Paths.get(BACKUP_DIR, TRACKER_BASE + currentProfile + JSON_EXTENSION);
            Path dest = Paths.get(getCurrentTrackerFilePath());
            if (Files.exists(backupFile)) {
                Files.copy(backupFile, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error restoring tracker profile backup: " + e.getMessage());
        }
        return false;
    }

    protected static void tryRestore() {
        try {
            List<String> profiles = getAvailableProfiles();
            for (String profile : profiles) {
                setCurrentProfile(profile);
                restoreBackup();
            }
        } catch (Exception e) {
            System.err.println("[FishyConfig] Error restoring tracker profile backups: " + e.getMessage());
        }
    }

    // --- Tracker profile management ---
    private static final String DEFAULT_PROFILE = "default";
    private static String currentProfile = DEFAULT_PROFILE;

    public static void saveProfile() {
        if (!DEFAULT_PROFILE.equals(currentProfile)) {
            saveToJson();
        }
    }

    private static String getCurrentTrackerFilePath() {
        return TRACKER_BASE_PATH + TRACKER_BASE + currentProfile + JSON_EXTENSION;
    }
    
    private static String getTrackerFilePath(String profile) {
        return TRACKER_BASE_PATH + TRACKER_BASE + profile + JSON_EXTENSION;
    }
    
    public static void setCurrentProfile(String profile) {
        if (profile == null || profile.trim().isEmpty()) {
            profile = DEFAULT_PROFILE;
        }
        saveToJson();
        currentProfile = profile.toLowerCase().replaceAll(PROFILE_NAME_PATTERN, "");
        ItemTrackerData.clearAll();
        loadFromJson();
    }
    
    public static String getCurrentProfile() {
        return currentProfile;
    }
    
    public static java.util.List<String> getAvailableProfiles() {
        java.util.List<String> profiles = new java.util.ArrayList<>();
        try {
            Path trackerDir = Paths.get(TRACKER_BASE_PATH);
            if (Files.exists(trackerDir)) {
                try (java.util.stream.Stream<Path> stream = Files.list(trackerDir)) {
                    stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith(TRACKER_BASE) && 
                                       path.getFileName().toString().endsWith(JSON_EXTENSION))
                        .forEach(path -> {
                            String fileName = path.getFileName().toString();
                            String profileName = fileName.substring(TRACKER_BASE.length(), fileName.length() - JSON_EXTENSION.length());
                            profiles.add(profileName);
                        });
                }
            }
        } catch (Exception e) {
            System.err.println("Error listing profiles: " + e.getMessage());
        }
        
        if (!profiles.contains(DEFAULT_PROFILE)) {
            profiles.add(0, DEFAULT_PROFILE);
        }
        
        return profiles;
    }
    
    public static boolean createProfile(String profileName) {
        if (profileName == null || profileName.trim().isEmpty()) {
            return false;
        }
        
        String cleanProfileName = profileName.toLowerCase().replaceAll(PROFILE_NAME_PATTERN, "");
        if (cleanProfileName.isEmpty()) {
            return false;
        }
        
        if (getAvailableProfiles().contains(cleanProfileName)) {
            return false;
        }
        
        saveToJson();
        setCurrentProfile(cleanProfileName);
        return true;
    }

    // Delete profile and switch to default if needed
    public static boolean deleteProfile(String profile) {
        if (DEFAULT_PROFILE.equals(profile)) {
            return false;
        }

        boolean wasActiveProfile = profile.equals(currentProfile);
        if (wasActiveProfile) {
            FishyNotis.alert(Text.literal("§8§oSwitched profile to default."));
            setCurrentProfile(DEFAULT_PROFILE);
        }
        
        try {
            Path filePath = Paths.get(getTrackerFilePath(profile));
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error deleting profile " + profile + ": " + e.getMessage());
        }
        return false;
    }
    
    public static void saveToJson() {
        try {
            Path filePath = Paths.get(getCurrentTrackerFilePath());
            Files.createDirectories(filePath.getParent());

            TrackerData data = new TrackerData();
            data.itemCounts = new HashMap<>(ItemTrackerData.getAllItems());
            data.sessionStartTime = ItemTrackerData.getSessionStartTime();
            data.lastActivityTime = ItemTrackerData.getLastActivityTime();
            data.totalPausedTime = ItemTrackerData.getTotalPausedTime();
            data.savedAt = System.currentTimeMillis();
            data.profileName = currentProfile;

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(data);

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write(json);
            }
        } catch (Exception e) {
            System.err.println("Error saving tracker data: " + e.getMessage());
        }
    }

    public static void saveOrCreate(String newProfileName) {
    if (currentProfile.equals(DEFAULT_PROFILE)) {
        saveToJson();
        boolean isUserInput = true;

        // Add timestamp if not user input
        if (newProfileName == null || newProfileName.trim().isEmpty()) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            newProfileName = TRACKER_BASE + timeStamp.substring(4).replaceFirst("_", "");
            isUserInput = false;
        }

        newProfileName = newProfileName + JSON_EXTENSION;
        Path newFilePath = Paths.get(TRACKER_BASE_PATH, newProfileName);
        Path defaultFilePath = Paths.get(getCurrentTrackerFilePath());

        try {
            Files.createDirectories(newFilePath.getParent());
            Files.copy(defaultFilePath, newFilePath);

            String newName = newProfileName.replace(TRACKER_BASE, "").replace(JSON_EXTENSION, "");
            String oldProfile = currentProfile;
            currentProfile = newName;

            loadFromJson();
            saveToJson();

            currentProfile = oldProfile;
            
            ItemTrackerData.clearAll();
            setCurrentProfile(newName);

            FishyNotis.send(Text.literal("§aCreated new tracker profile: " + newName));

            if (!isUserInput) {
                FishyNotis.alert(Text.literal("§7Use /fp profile rename " + newName + " <new_name> to rename it."));
            }
        } catch (Exception e) {
            System.err.println("Error creating new tracker data file: " + e.getMessage());
        }
    } else {
        saveToJson();
    }
}    

    public static boolean renameProfile(String oldName, String newName) {
        if (oldName == null || newName == null || oldName.trim().isEmpty() || newName.trim().isEmpty()) {
            FishyNotis.alert(Text.literal("§cInvalid profile name(s)"));
            return false;
        }

        String cleanOldName = oldName.toLowerCase().replaceAll(PROFILE_NAME_PATTERN, "");
        String cleanNewName = newName.toLowerCase().replaceAll(PROFILE_NAME_PATTERN, "");
        if (cleanOldName.isEmpty() || cleanNewName.isEmpty()) {
            return false;
        }

        if (!getAvailableProfiles().contains(cleanOldName) || getAvailableProfiles().contains(cleanNewName)) {
            return false;
        }

        try {
            Path oldFilePath = Paths.get(getTrackerFilePath(cleanOldName));
            Path newFilePath = Paths.get(getTrackerFilePath(cleanNewName));
            Files.move(oldFilePath, newFilePath);
            String json = Files.readString(newFilePath);
            json = json.replace("\"profileName\": \"" + cleanOldName + "\"", "\"profileName\": \"" + cleanNewName + "\"");
            Files.writeString(newFilePath, json);

            if (Files.exists(oldFilePath)) {
                System.err.println("Warning: Old profile file still exists after rename.");
                Files.delete(oldFilePath);
            }

            return true;
        } catch (Exception e) {
            System.err.println("Error renaming profile " + oldName + " to " + newName + ": " + e.getMessage());
        }
        return false;
    }

    public static boolean loadFromJson() {
        try {
            Path filePath = Paths.get(getCurrentTrackerFilePath());
            if (!Files.exists(filePath)) {
                return false;
            }

            String json = Files.readString(filePath);
            Gson gson = new Gson();
            TrackerData data = gson.fromJson(json, TrackerData.class);

            if (data != null && data.itemCounts != null) {
                ItemTrackerData.setAllItems(data.itemCounts);
                if (data.sessionStartTime > 0) {
                    ItemTrackerData.setSessionStartTime(data.sessionStartTime);
                }
                if (data.lastActivityTime > 0) {
                    ItemTrackerData.setLastActivityTime(data.lastActivityTime);
                } else {
                    ItemTrackerData.setLastActivityTime(System.currentTimeMillis());
                }
                ItemTrackerData.setTotalPausedTime(data.totalPausedTime);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error loading tracker data: " + e.getMessage());
        }
        return false;
    }
    
    public static boolean deleteJsonFile() {
        try {
            Path filePath = Paths.get(getCurrentTrackerFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error deleting tracker data file: " + e.getMessage());
        }
        return false;
    }
    
    public static boolean hasJsonFile() {
        return Files.exists(Paths.get(getCurrentTrackerFilePath()));
    }    

    private static class TrackerData {
        Map<String, Integer> itemCounts;
        long sessionStartTime;
        long lastActivityTime;
        long totalPausedTime;
        long savedAt;
        String profileName;
    }

    private TrackerProfiles() {
        throw new UnsupportedOperationException("Configuration class");
    }
}
