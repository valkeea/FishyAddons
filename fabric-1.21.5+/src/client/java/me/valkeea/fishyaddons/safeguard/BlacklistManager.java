package me.valkeea.fishyaddons.safeguard;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import me.valkeea.fishyaddons.config.ItemConfig;

public class BlacklistManager {
    private BlacklistManager() {}
    
    // --- Entry class ---
    public static class GuiBlacklistEntry {
        public final List<String> identifiers;
        private boolean enabled;
        public final boolean checkTitle;

        public GuiBlacklistEntry(List<String> identifiers, boolean enabled, boolean checkTitle) {
            this.identifiers = identifiers;
            this.enabled = enabled;
            this.checkTitle = checkTitle;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    // --- Blacklist data ---
    private static final List<GuiBlacklistEntry> defaultBlacklist = new ArrayList<>();
    private static final List<GuiBlacklistEntry> userBlacklist = new CopyOnWriteArrayList<>();
    private static boolean loaded = false;

    static {
        defaultBlacklist.add(new GuiBlacklistEntry(Arrays.asList("Create Auction", "Create BIN Auction", "Auction House"), true, true));
        defaultBlacklist.add(new GuiBlacklistEntry(Arrays.asList("Coins Transaction"), true, false));
        defaultBlacklist.add(new GuiBlacklistEntry(Arrays.asList("Salvage Items"), true, true));
        defaultBlacklist.add(new GuiBlacklistEntry(Arrays.asList("Sell Item", "Click items in your inventory to sell", "Click to buyback"), true, false));
        loadUserBlacklist();
    }

    // --- Blacklist logic ---
    public static List<GuiBlacklistEntry> getMergedBlacklist() {
        List<GuiBlacklistEntry> merged = new ArrayList<>();
        for (GuiBlacklistEntry def : defaultBlacklist) {
            GuiBlacklistEntry override = findUserOverride(def);
            if (override != null) {
                merged.add(new GuiBlacklistEntry(def.identifiers, override.isEnabled(), def.checkTitle));
            } else {
                merged.add(def);
            }
        }
        for (GuiBlacklistEntry userEntry : userBlacklist) {
            boolean overridesExisting = false;
            for (GuiBlacklistEntry def : defaultBlacklist) {
                if (matchesIdentifier(def, userEntry.identifiers.get(0))) {
                    overridesExisting = true;
                    break;
                }
            }
            if (!overridesExisting) {
                merged.add(userEntry);
            }
        }
        return merged;
    }

    public static List<GuiBlacklistEntry> getUserBlacklist() {
        ensureLoaded();
        return new ArrayList<>(userBlacklist);
    }

    public static void updateBlacklistEntry(String identifier, boolean enabled) {
        ensureLoaded();
        for (GuiBlacklistEntry entry : userBlacklist) {
            for (String id : entry.identifiers) {
                if (id.equalsIgnoreCase(identifier)) {
                    entry.setEnabled(enabled);
                    saveUserBlacklist();
                    return;
                }
            }
        }
        userBlacklist.add(new GuiBlacklistEntry(Collections.singletonList(identifier), enabled, false));
        saveUserBlacklist();
    }

    private static GuiBlacklistEntry findUserOverride(GuiBlacklistEntry def) {
        for (String id : def.identifiers) {
            for (GuiBlacklistEntry userEntry : userBlacklist) {
                if (matchesIdentifier(userEntry, id)) {
                    return userEntry;
                }
            }
        }
        return null;
    }

    private static boolean matchesIdentifier(GuiBlacklistEntry entry, String identifier) {
        for (String id : entry.identifiers) {
            if (id.equalsIgnoreCase(identifier)) {
                return true;
            }
        }
        return false;
    }

    public static void saveUserBlacklist() {
        getUserBlacklist();
        ItemConfig.save();
    }  

    public static synchronized void loadUserBlacklist() {
        userBlacklist.clear();
        loaded = true;
    }

    // --- JSON helpers (for config serialization) ---
    public static List<Map<String, Object>> getUserBlacklistAsJson() {
        ensureLoaded();
        List<Map<String, Object>> jsonList = new ArrayList<>();
        for (GuiBlacklistEntry entry : userBlacklist) {
            Map<String, Object> map = new HashMap<>();
            map.put("identifiers", entry.identifiers);
            map.put("enabled", entry.isEnabled());
            map.put("checkTitle", entry.checkTitle);
            jsonList.add(map);
        }
        return jsonList;
    }

    public static void loadUserBlacklistFromJson(List<Map<String, Object>> jsonEntries) {
        userBlacklist.clear();
        for (Map<String, Object> entry : jsonEntries) {
            Object idObj = entry.get("identifiers");
            List<String> identifiers = new ArrayList<>();
            if (idObj instanceof List<?>) {
                for (Object o : (List<?>) idObj) {
                    if (o instanceof String string) {
                        identifiers.add(string);
                    }
                }
            }
            boolean enabled = Boolean.TRUE.equals(entry.get("enabled"));
            boolean checkTitle = Boolean.TRUE.equals(entry.get("checkTitle"));
            userBlacklist.add(new GuiBlacklistEntry(identifiers, enabled, checkTitle));
        }
        loaded = true;
    }

    private static void ensureLoaded() {
        if (!loaded) loadUserBlacklist();
    }
}
