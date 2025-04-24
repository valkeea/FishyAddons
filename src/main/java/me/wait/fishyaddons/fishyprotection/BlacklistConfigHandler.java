package me.wait.fishyaddons.fishyprotection;

import me.wait.fishyaddons.util.GuiBlacklistEntry;
import me.wait.fishyaddons.config.UUIDConfigHandler;
import net.minecraft.util.StringUtils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlacklistConfigHandler {

    private static final List<GuiBlacklistEntry> cachedUserBlacklist = new CopyOnWriteArrayList<>();
    private static boolean loaded = false;

    public static synchronized List<GuiBlacklistEntry> getUserBlacklist() {
        if (!loaded) loadUserBlacklist();
        return new ArrayList<>(cachedUserBlacklist);
    }

    public static synchronized void updateBlacklistEntry(String identifier, boolean enabled) {
        if (!loaded) loadUserBlacklist();

        for (GuiBlacklistEntry entry : cachedUserBlacklist) {
            for (String id : entry.identifiers) {
                if (id.equalsIgnoreCase(identifier)) {
                    entry.enabled = enabled;
                    saveUserBlacklist();
                    return;
                }
            }
        }

        cachedUserBlacklist.add(new GuiBlacklistEntry(Collections.singletonList(identifier), enabled, false));
        saveUserBlacklist();
    }

    public static synchronized void loadUserBlacklist() {
        cachedUserBlacklist.clear();
        List<GuiBlacklistEntry> loadedList = BlacklistStore.getUserBlacklist();
        if (loadedList != null) {
            cachedUserBlacklist.addAll(loadedList);
        }
        loaded = true;
    }

    public static synchronized void saveUserBlacklist() {
        BlacklistStore.saveUserBlacklist();
    }

    public static synchronized void loadUserBlacklistFromJson(List<Map<String, Object>> jsonEntries) {
        cachedUserBlacklist.clear();
        for (Map<String, Object> entryMap : jsonEntries) {
            Object identifiersObj = entryMap.get("identifiers");
            Object enabledObj = entryMap.get("enabled");
            Object checkTitleObj = entryMap.get("checkTitle");

            if (identifiersObj instanceof List && enabledObj instanceof Boolean && checkTitleObj instanceof Boolean) {
                List<String> identifiers = new ArrayList<>();
                for (Object idObj : (List<?>) identifiersObj) {
                    if (idObj instanceof String) {
                        identifiers.add((String) idObj);
                    }
                }

                cachedUserBlacklist.add(new GuiBlacklistEntry(identifiers, (Boolean) enabledObj, (Boolean) checkTitleObj));
            }
        }
        loaded = true;
    }

    public static synchronized List<Map<String, Object>> getUserBlacklistAsJson() {
        List<Map<String, Object>> jsonList = new ArrayList<>();
        for (GuiBlacklistEntry entry : cachedUserBlacklist) {
            Map<String, Object> map = new HashMap<>();
            map.put("identifiers", entry.identifiers);
            map.put("enabled", entry.enabled);
            map.put("checkTitle", entry.checkTitle);
            jsonList.add(map);
        }
        return jsonList;
    }
    
}