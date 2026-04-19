package me.valkeea.fishyaddons.feature.item.safeguard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.StringKey;

public class BlacklistManager {
    private BlacklistManager() {}

    public static class BlacklistEntry {
        public final String identifier;
        public final String displayName;
        public final List<String> matchPatterns;
        public final boolean checkTitle;

        public BlacklistEntry(String identifier, String displayName, List<String> matchPatterns, boolean checkTitle) {
            this.identifier = identifier;
            this.displayName = displayName;
            this.matchPatterns = matchPatterns;
            this.checkTitle = checkTitle;
        }

        public boolean isEnabled() {
            return !getDisabledIdentifiers().contains(identifier);
        }
    }

    private static final List<BlacklistEntry> ALL_ENTRIES = new ArrayList<>();

    public static void init() {
        ALL_ENTRIES.add(new BlacklistEntry(
            "auction_house",
            "Auction House",
            Arrays.asList("Create Auction", "Create BIN Auction", "Co-op Auction House", "Auction House"),
            true
        ));
        ALL_ENTRIES.add(new BlacklistEntry(
            "player_trades",
            "Player Trades",
            Arrays.asList("Coins Transaction"),
            false
        ));
        ALL_ENTRIES.add(new BlacklistEntry(
            "salvaging",
            "Salvaging",
            Arrays.asList("Salvage Items"),
            true
        ));
        ALL_ENTRIES.add(new BlacklistEntry(
            "npc_sales",
            "NPC Sales",
            Arrays.asList("Click items in your inventory to sell", "Click to buyback"),
            false
        ));
    }

    /**
     * Returns all GUI blacklist entries.
     */
    public static List<BlacklistEntry> getAllEntries() {
        return new ArrayList<>(ALL_ENTRIES);
    }

    /**
     * Returns only the enabled GUI blacklist entries.
     */
    public static List<BlacklistEntry> getEnabledEntries() {
        Set<String> disabled = getDisabledIdentifiers();
        List<BlacklistEntry> enabled = new ArrayList<>();
        for (var entry : ALL_ENTRIES) {
            if (!disabled.contains(entry.identifier)) {
                enabled.add(entry);
            }
        }
        return enabled;
    }

    /**
     * Checks if a GUI identifier is currently enabled (not disabled).
     */
    public static boolean isEnabled(String identifier) {
        return !getDisabledIdentifiers().contains(identifier);
    }

    /**
     * Toggles the enabled state of a GUI identifier.
     */
    public static void toggle(String identifier) {
        Set<String> disabled = getDisabledIdentifiers();
        if (disabled.contains(identifier)) {
            disabled.remove(identifier);
        } else {
            disabled.add(identifier);
        }
        saveDisabledIdentifiers(disabled);
    }

    private static Set<String> getDisabledIdentifiers() {
        String raw = Config.get(StringKey.BLACKLIST_EXCEPTIONS);
        Set<String> disabled = new HashSet<>();
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    disabled.add(trimmed);
                }
            }
        }
        return disabled;
    }

    private static void saveDisabledIdentifiers(Set<String> disabled) {
        String joined = String.join(",", disabled);
        Config.set(StringKey.BLACKLIST_EXCEPTIONS, joined);
    }
}
