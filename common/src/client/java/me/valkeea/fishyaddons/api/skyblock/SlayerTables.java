package me.valkeea.fishyaddons.api.skyblock;

import java.util.EnumMap;
import java.util.Map;

public class SlayerTables {
    
    public enum SlayerType {
        WOLF("Wolf","§3Wolf", "Wolves"),
        ZOMBIE("Zombie", "§cZombie", "Zombies"),
        SPIDER("Spider", "§2Spider", "Spiders"),
        ENDERMAN("Enderman", "§5Enderman", "Endermen"),
        BLAZE("Blaze", "§6Blaze", "Blazes"),
        VAMPIRE("Vampire", "§4Vampire", "Vampires");
        
        private final String displayName;
        private final String pluralName;
        private final String type;
        
        SlayerType(String type, String displayName, String pluralName) {
            this.type = type;
            this.displayName = displayName;
            this.pluralName = pluralName;
        }
        
        public String getPluralName() {
            return pluralName;
        }

        public String getCmdName() {
            return displayName;
        }

        public static SlayerType fromString(String text) {

            String lower = text.toLowerCase();
            for (SlayerType slayerType : values()) {
                if (lower.contains(slayerType.type.toLowerCase())) return slayerType;
            }
            return null;
        }
    }
    
    private static final Map<SlayerType, int[]> SPAWN_XP = new EnumMap<>(SlayerType.class);
    private static final Map<SlayerType, int[]> XP_GAINED = new EnumMap<>(SlayerType.class);
    
    static {

        SPAWN_XP.put(SlayerType.WOLF, new int[]{
            207,
            496,
            1210,
            2420
        });

        SPAWN_XP.put(SlayerType.ZOMBIE, new int[]{
            150,
            1140,
            2400,
            4800,
            7200
        });

        SPAWN_XP.put(SlayerType.SPIDER, new int[]{
            250,
            750,
            1500,
            3000,
            10000
        });

        SPAWN_XP.put(SlayerType.ENDERMAN, new int[]{
            2750,
            6600, 
            11000,
            22000
        });

        SPAWN_XP.put(SlayerType.BLAZE, new int[]{
            5600,
            13400,
            16800,
            33600
        });

        SPAWN_XP.put(SlayerType.VAMPIRE, new int[]{
            360,
            450,
            600,
            750,
            900
        });

        int[] base = {5, 25, 100, 500, 1500};
        XP_GAINED.put(SlayerType.WOLF, base);
        XP_GAINED.put(SlayerType.ZOMBIE, base);
        XP_GAINED.put(SlayerType.SPIDER, base);
        XP_GAINED.put(SlayerType.ENDERMAN, base);
        XP_GAINED.put(SlayerType.BLAZE, base);
        XP_GAINED.put(SlayerType.VAMPIRE, new int[]{10, 25, 60, 120, 150});
    }
    
    /**
     * Get the tier number (1-5) based on quest XP requirement
     * Returns -1 if XP doesn't match any tier
     */
    public static int getTierFromXp(SlayerType type, int xp) {
        int[] tiers = SPAWN_XP.get(type);
        if (tiers == null) {
            return -1;
        }
        
        for (int i = 0; i < tiers.length; i++) {
            if (tiers[i] == xp) {
                return i + 1;
            }
        }
        return -1;
    }
    
    /**
     * Get the XP value for a specific tier
     */
    public static int getXpForTier(SlayerType type, int tier) {
        int[] tiers = SPAWN_XP.get(type);
        if (tiers == null || tier < 1 || tier > tiers.length) {
            return 0;
        }
        return tiers[tier - 1];
    }
    
    /**
     * Parse XP from a quest start message
     */
    public static int parseXpFromMessage(String message) {
        try {
            String cleaned = message.replace("» Slay ", "")
                                   .replace(",", "")
                                   .replace("Combat XP worth of", "")
                                   .trim();
            
            String[] parts = cleaned.split(" ");
            if (parts.length > 0) {
                return Integer.parseInt(parts[0]);
            }
        } catch (NumberFormatException e) {
            // Failed to parse XP
        }
        return 0;
    }
    
    /**
     * Get the number of tiers for a slayer type
     */
    public static int getTierCount(SlayerType type) {
        int[] tiers = SPAWN_XP.get(type);
        return tiers != null ? tiers.length : 0;
    }
    
    /**
     * Get the slayer XP gained for completing a specific tier
     */
    public static int getXpGained(SlayerType type, int tier) {
        int[] xpGained = XP_GAINED.get(type);
        if (xpGained == null || tier < 1 || tier > xpGained.length) {
            return 0;
        }
        return xpGained[tier - 1];
    }
    
    private SlayerTables() {}
}
