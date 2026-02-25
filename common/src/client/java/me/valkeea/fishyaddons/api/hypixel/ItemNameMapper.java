package me.valkeea.fishyaddons.api.hypixel;

public class ItemNameMapper {
    
    private static final String[] RARITIES = {"mythic", "legendary", "epic", "rare", "uncommon", "common"};
    
    private static final String[] REFORGE_PREFIXES = {
        "Sharp", "Heroic", "Spicy", "Legendary", "Fabled", "Withered", "Ancient",
        "Necrotic", "Pleasant", "Precise", "Spiritual", "Headstrong", "Clean",
        "Fierce", "Heavy", "Light", "Mythic", "Pure", "Smart", "Titanic",
        "Wise", "Perfect", "Refined", "Blessed", "Fruitful", "Magnetic",
        "Fleet", "Stellar", "Heated", "Ambered", "Keen", "Strong", "Festive", 
        "Submerged", "Mossy"
    };
    
    private ItemNameMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Check if an item name represents a tiered drop (rarity prefix + base name).
     */
    public static boolean isTieredDrop(String itemName) {
        if (itemName == null) return false;
        String lower = itemName.toLowerCase().trim();
        
        for (String rarity : RARITIES) {
            if (lower.startsWith(rarity + " ")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Split tiered drop name into base name and rarity.
     */
    public static String[] parseNameAndRarity(String tieredDropName) {
        if (tieredDropName == null) return new String[0];
        
        String lower = tieredDropName.toLowerCase().trim();
        
        for (String rarity : RARITIES) {
            if (lower.startsWith(rarity + " ")) {
                String baseName = lower.substring(rarity.length() + 1).trim();
                return new String[]{baseName, rarity};
            }
        }
        
        return new String[0];
    }
    
    // API ID conversion (BZ normalization)
    
    /**
     * Convert display name or normalized name to API ID for bazaar lookup.
     */
    public static String toApiId(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) return "";
        
        var name = itemName.trim();
        var directMapping = getDirectMapping(name);
        if (directMapping != null) return directMapping;

        var upper = name.toUpperCase();
        if (upper.contains("SHARD") && 
            !upper.contains("SHARD_OF_THE_SHREDDED") &&
            !upper.contains("PRISMARINE") &&
            !upper.contains("PRISMARINE_SHARD")) {
            return convertShardToApiId(upper);
        }
        
        if (isEnchantment(upper)) return convertEnchantmentToApiId(upper);

        var normalized = upper.replaceAll("[-\\s]+", "_");
        var knownPattern = getKnownPattern(normalized);

        if (knownPattern != null) return knownPattern;
        return normalized;
    }
    
    /**
     * Convert shard name to API ID format: SHARD_<MOB_NAME>
     */
    private static String convertShardToApiId(String shardName) {
        var mobName = shardName
            .replace(" SHARD", "")
            .replace(" SHARDS", "")
            .replaceAll("\\s+", "_")
            .toUpperCase();
        
        var shardApiId = "SHARD_" + mobName;
        
        if (shardApiId.equals("SHARD_LOCH_EMPEROR")) {
            return "SHARD_SEA_EMPEROR"; // Emps feel unique
        }
        
        return shardApiId;
    }

    /**
     * Check if item name is an enchantment book.
     */
    private static boolean isEnchantment(String upper) {
        var hasValidLevel = upper.matches(".*\\b([1-9]|10)\\b.*");
        var hasEnchantWords = upper.contains("BOOK") || upper.contains("ULTIMATE");
        return hasValidLevel && hasEnchantWords;
    }
    
    /**
     * Convert enchantment name to API ID format: ENCHANTMENT_<NAME>
     */
    private static String convertEnchantmentToApiId(String enchantName) {
        return "ENCHANTMENT_" + enchantName.replaceAll("[-\\s]+", "_").toUpperCase();
    }
    
    // Display name cleanup (AH normalization)
    
    /**
     * Clean up display name for auction lookup.
     * Removes color codes, reforges, stars, level prefixes, etc.
     */
    public static String cleanDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) return "";
        
        var cleaned = displayName.replaceAll("§[0-9a-fk-or]", "");
        
        if (cleaned.matches("\\[Lvl \\d+\\].*")) { // Remove level prefix if present
            cleaned = cleaned.replaceAll("\\[Lvl \\d+\\]\\s*", "");
        }
        
        for (String prefix : REFORGE_PREFIXES) {
            if (cleaned.startsWith(prefix + " ")) {
                cleaned = cleaned.substring(prefix.length() + 1);
                break; // Remove reforge prefixes if present
            }
        }
        
        cleaned = cleaned.replaceAll("[✪➤◆⚚]+", "").trim();
        cleaned = cleaned.replaceAll("\\s*\\+\\d+\\s*$", "").trim();
        return cleaned.replaceAll("\\s*\\([^)]+\\)\\s*$", "").trim();
    }
    
    // --- Unconventional Items ---
    
    /**
     * Get direct API ID mapping for special items.
     * Returns null if no special mapping exists.
     */
    private static String getDirectMapping(String itemName) {
        return switch (itemName) {
            case "agathas coupon" -> "AGATHA_COUPON";
            case "experience bottle" -> "EXPERIENCE_BOTTLE";
            case "grand experience bottle" -> "GRAND_EXP_BOTTLE";
            case "titanic experience bottle" -> "TITANIC_EXP_BOTTLE";
            case "colossal experience bottle" -> "COLOSSAL_EXP_BOTTLE";
            case "lily pad" -> "WATER_LILY";
            case "enchanted lily pad" -> "ENCHANTED_WATER_LILY";
            case "raw salmon" -> "RAW_FISH:1";
            case "clownfish" -> "RAW_FISH:2";
            case "pufferfish" -> "RAW_FISH:3";
            case "enchanted raw cod" -> "ENCHANTED_RAW_FISH";
            case "ink sac" -> "INK_SACK";
            case "cactus green" -> "INK_SACK:2";
            case "cocoa beans" -> "INK_SACK:3";
            case "hay bale" -> "HAY_BLOCK";
            case "emperors skull" -> "DIVER_FRAGMENT";
            case "thunder fragment" -> "THUNDER_SHARDS";
            case "sunflower" -> "DOUBLE_PLANT"; // help
            case "duplex i" -> "ENCHANTMENT_ULTIMATE_REITERATE_1";
            case "magmafish" -> "MAGMA_FISH";
            case "silver magmafish" -> "MAGMA_FISH_SILVER";
            case "gold magmafish" -> "MAGMA_FISH_GOLD";
            case "diamond magmafish" -> "MAGMA_FISH_DIAMOND";
            case "lucky clover core" -> "PET_ITEM_LUCKY_CLOVER_DROP";            
            case "exp share core" -> "PET_ITEM_EXP_SHARE_CORE_DROP";
            case "coins" -> null;
            default -> null;
        };
    }

    /**
     * Check for known patterns that can be normalized to API IDs.
     */
    private static String getKnownPattern(String n) {
        if (n.contains("GEMSTONE")) return n.replace("GEMSTONE", "GEM");
        if (n.contains("INGOT")) return n.replace("_INGOT", "");
        if (n.equals("POTATO") || n.equals("CARROT")) return n + "_ITEM";
        return null;
    }
}
