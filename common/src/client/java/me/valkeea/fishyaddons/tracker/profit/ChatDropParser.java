package me.valkeea.fishyaddons.tracker.profit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.cache.ApiCache;

public class ChatDropParser {
    private static final String SHARD_KEYWORD = "shard";
    private static final String SHARD_PLURAL = " shards";
    private static final String COINS_KEYWORD = "coins";
    private static final String COIN_KEYWORD = "coin";
    private static final String CHARM_KEYWORD = "charm";
    private static final String NAGA_KEYWORD = "naga";
    private static final String YOU_CAUGHT = "you caught";
    private static final String RARE_DROP = "rare drop";
    private static final String VERY_RARE_DROP = "very rare drop";
    private static final String CRAZY_RARE_DROP = "crazy rare drop";
    private static final String EXTREMELY_RARE_DROP = "extremely rare drop";
    private static final String INSANE_DROP = "insane drop";
    private static final String PET_DROP = "pet drop";
    private static final String PRAY_TO_RNGESUS_DROP = "pray to rngesus drop";
    private static final String SALT_YOU_CHARMED = "salt you charmed";
    private static final String LOOT_SHARE = "loot share";
    private static final String ANY_CATCH = "⛃\\s+\\w+\\s+catch!\\s+you caught\\s+";
    private static final String CATCH_KEYWORD = "catch!";
    private static final String WOW_DUG_OUT = "wow! you dug out";
    private static final String DROP_REGEX_SUFFIX = "!\\s*([^(\\r\\n]+?)(?:\\s*\\([^)]*\\).*)?$";
    private static final List<DropPattern> DROP_PATTERNS = new ArrayList<>();
    private static final Set<String> TRACKED_SHARDS = new HashSet<>();
    private static final Set<String> VALUABLE_KEYWORDS = new HashSet<>();
    
    static {
        init();
        initPatterns();
    }
    
    private static void init() {
        String[] shardNames = {

            // Murkwater/Galatea
            "spike", "pandarai", "bullfrog", "beaconmite", "bambloom", "mochibear", "seagull", 
            "bambuleaf", "heron", "sparrow", "crow", "birries", "phanflare", "phanpyre",
            "carvernshade", "bal", "quartzfang", "troglobyte", "miner zombie", "hideonleaf",
            "dodo", "condor", "toucan", "falcon", "rana", "kiwi", "boreal owl", "lapis skeleton",
            "megalith", "naga", "tortoise", "wyvern", "tiamat", "chameleon", "leatherback",
            "sea serpent", "caiman", "komodo dragon", "iguana", "moray eel", "basilisk",
            "fenlord", "alligator", "leviathan", "gecko", "king cobra", "eel", "crocodile",
            "python", "lizard king", "toad", "viper", "mossybit", "cuboa", "salamander",
            "newt", "harpy", "tadgang", "starborn", "molthorn", "galaxy fish", "etherdrake",
            "jormurg", "sun fish", "hideonbox", "ananke", "moltenfish", "daemon", "shellwise",
            "prince", "hideonsack", "wither", "inferno koi", "draconic", "joydive", "dreadwing",
            "lumisquid", "carrot king", "snowfin", "hideonring", "silentdepth", "revenant",
            "bitbug", "arachne", "abyssal lanternfish", "hideondra", "hideongeon", "piranha",
            "glacite walker", "hideoncave", "hideongift", "tempest", "aero", "bolt",
            "quake", "flash",
            
            // Mobs
            "apex dragon", "cinderbat", "kraken", "blizzard", "cyro", "cascade", "tide",
            "mist", "xyz", "shinyfish", "endstone protector", "tenebris", "terra", "sylvan",
            "bramble", "grove", "thorn", "kada knight", "wither spectre", "obsidian defender",
            "bezal", "zealot", "voracious spider", "tank zombie", "lapis zombie", "barbarian duke",
            "skeletor", "burningsoul", "flare", "soul of the alpha", "power dragon", "bruiser",
            "matcho", "sycophant", "rain slime", "golden ghoul", "zombie soldier", 
            "flaming spider", "lava flame", "loch emperor", "magma slug", "lord jawbus",
            "water hydra", "fire eel", "sea archer", "night squid", "taurus", "lapis creeper",
            "ghost", "hellwisp", "mimc", "fungloom", "stalagmight", "thyst", "star sentry",
            "drowned", "stridersurfer", "ent", "seer", "azure", "chill", "salmon", "goldfin",
            "coralot", "verdant", "cod", "cretan bull", "minotaur", "king minos", "sphinx", "yog",
            
            // Bugs
            "wartybug", "dragonfly", "firefly", "lunar moth", "ladybug", "cropeetle",
            "invisibug", "termite", "praying mantis", "pest", "mudworm"
        };
        
        for (String shard : shardNames) {
            TRACKED_SHARDS.add(shard.toLowerCase());
        }
        
        String[] keywords = {
            "enchanted", "rare", "epic", "legendary",
            "book", "talisman", "[Lvl 1]", COIN_KEYWORD, SHARD_KEYWORD
        };
        
        for (String keyword : keywords) {
            VALUABLE_KEYWORDS.add(keyword.toLowerCase());
        }
    }
    
    private static void initPatterns() {
        // Pattern 1a: "RARE DROP! You dug out ItemName!"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(RARE_DROP + "!\\s*You dug out an? ([^!]+)!", Pattern.CASE_INSENSITIVE),
            -1, 1
        ));

        // Pattern 5a: X DROP! Enchanted Book ItemName (+x ✯ Magic Find)
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile("(?:rare drop|very rare drop|crazy rare drop|extremely rare drop|insane drop)!\\s*Enchanted Book \\(([^)]+)\\)\\s*\\(\\+\\d+ ✯ Magic Find\\)", Pattern.CASE_INSENSITIVE),
            -1, 1
        ));

        // Pattern 5b: "X DROP! (31x ItemName) (+Magic Find)"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(
                "(?:rare drop|very rare drop|crazy rare drop|extremely rare drop|insane drop)!\\s*\\((\\d+)x\\s+([^)]+?)\\)(?:\\s*\\([^)]*\\))?.*?$", 
                Pattern.CASE_INSENSITIVE),
            1, 2  // quantity group 1, item group 2
        ));

        // Pattern 5c: "X DROP! (ItemName) (+Magic Find)"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(
                "(?:rare drop|very rare drop|crazy rare drop|extremely rare drop|insane drop)!\\s*\\((?!\\d+x\\s)([^)]+?)\\)(?:\\s*\\([^)]*\\))?.*?$", 
                Pattern.CASE_INSENSITIVE),
            -1, 1  // no quantity group, item group 1
        ));

        // Pattern 1: "RARE DROP! ItemName"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(RARE_DROP + DROP_REGEX_SUFFIX, Pattern.CASE_INSENSITIVE),
            -1, 1 // quantity is 1
        ));
        
        // Pattern 2: "VERY RARE DROP! ItemName"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(VERY_RARE_DROP + DROP_REGEX_SUFFIX, Pattern.CASE_INSENSITIVE),
            -1, 1
        ));
        
        // Pattern 3: "CRAZY RARE DROP! ItemName"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(CRAZY_RARE_DROP + DROP_REGEX_SUFFIX, Pattern.CASE_INSENSITIVE),
            -1, 1
        ));
        
        // Pattern 4: "EXTREMELY RARE DROP! ItemName"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(EXTREMELY_RARE_DROP + DROP_REGEX_SUFFIX, Pattern.CASE_INSENSITIVE),
            -1, 1
        ));
        
        // Pattern 5: "INSANE DROP! ItemName"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(INSANE_DROP + DROP_REGEX_SUFFIX, Pattern.CASE_INSENSITIVE),
            -1, 1
        ));

        // Pattern 6: "PRAY TO RNGESUS DROP! ItemName"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(PRAY_TO_RNGESUS_DROP + DROP_REGEX_SUFFIX, Pattern.CASE_INSENSITIVE),
            -1, 1
        ));
        
        // Pattern 7: "PET DROP! ItemName"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(PET_DROP + DROP_REGEX_SUFFIX, Pattern.CASE_INSENSITIVE),
            -1, 1
        ));
        
        // Pattern 8b: "⛃ <tier> CATCH! You caught ItemName xX!" - Multiple items (MUST come before 8a)
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(ANY_CATCH + "(.+?)\\s+x(\\d{1,3})!", Pattern.CASE_INSENSITIVE),
            2, 1  // quantity group 2, item group 1
        ));

        // Pattern 8a: "⛃ <tier> CATCH! You caught a/an ItemName!" - Single item (MUST come after 8b)
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(ANY_CATCH + "(?:an?\\s+)?(.+?)!", Pattern.CASE_INSENSITIVE),
            -1, 1
        ));
        
        // Pattern 11: "Wow! You dug out X coins!"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(WOW_DUG_OUT + " ([\\d,]+)\\s+coins!", Pattern.CASE_INSENSITIVE),
            -2, 1 // Special marker for coins
        ));
        
        // Pattern 12: "You caught xX ItemName Shards!"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile("you caught x(\\d+)\\s+(.+?)\\s*shards?!", Pattern.CASE_INSENSITIVE),
            1, 2
        ));
        
        // Pattern 13: "You caught a/an ItemName Shard!"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile("you caught an?\\s+(.+?)\\s*shard!", Pattern.CASE_INSENSITIVE),
            -1, 1
        ));
        
        // Pattern 14: "CHARM/SALT/NAGA You charmed a CreatureName and captured X Shards from it."
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile("(?:salt|charm|naga)\\s+you charmed an?\\s+(.+?)\\s+and captured (\\d+)\\s+shards? from it.", Pattern.CASE_INSENSITIVE),
            2, 1
        ));
        
        // Pattern 14b: "CHARM/SALT/NAGA You charmed a CreatureName and captured its Shard."
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile("(?:salt|charm|naga)\\s+you charmed an?\\s+(.+?)\\s+and captured its shard.", Pattern.CASE_INSENSITIVE),
            -1, 1
        ));
        
        // Pattern 15: "LOOT SHARE You received a ItemName Shard for assisting <someone>!"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile("loot share\\s+you received an?\\s+(.+?)\\s*shard for assisting\\s+\\w+!", Pattern.CASE_INSENSITIVE),
            -1, 1
        ));
    }

    public static ParseResult parseMessage(String message) {
        if (message == null || message.trim().isEmpty()) return null;
        
        // Check cache first for previously parsed messages
        Object cachedResult = ApiCache.getCachedMessageParse(message);
        if (cachedResult != null) {
            if (ApiCache.isNullParseResult(cachedResult)) {
                return null;
            }
            return (ParseResult) cachedResult;
        }
        
        // Filter out annoying cases (mostly fishing) 
        if (ignore(message)) {
            ApiCache.cacheMessageParse(message, null);
            return null;
        }
        
        // Quick pre-filter to check for common drop indicators
        if (!isDrop(message)) {
            ApiCache.cacheMessageParse(message, null);
            return null;
        }
        
        // Remove color codes and clean the message
        for (DropPattern pattern : DROP_PATTERNS) {
            ParseResult result = tryPattern(pattern, message);
            if (result != null) {
                // Cache the successful parse result
                ApiCache.cacheMessageParse(message, result);
                return result;
            }
        }
        
        ApiCache.cacheMessageParse(message, null);
        return null;
    }

    private static boolean isDrop(String lowerMessage) {
        return lowerMessage.contains(RARE_DROP) ||
               lowerMessage.contains(VERY_RARE_DROP) ||
               lowerMessage.contains(CRAZY_RARE_DROP) ||
               lowerMessage.contains(EXTREMELY_RARE_DROP) ||
               lowerMessage.contains(INSANE_DROP) ||
               lowerMessage.contains(PRAY_TO_RNGESUS_DROP) ||
               lowerMessage.contains(PET_DROP) ||
               lowerMessage.contains(CATCH_KEYWORD) ||
               lowerMessage.contains(WOW_DUG_OUT) ||
               lowerMessage.contains(YOU_CAUGHT) ||
               lowerMessage.contains(CHARM_KEYWORD) ||
                lowerMessage.contains(NAGA_KEYWORD) ||
               lowerMessage.contains(SALT_YOU_CHARMED) ||
               lowerMessage.contains(LOOT_SHARE);
    }

    private static boolean ignore(String lowerMessage) {
        if (lowerMessage.contains(CATCH_KEYWORD) ||
            lowerMessage.contains(SHARD_KEYWORD) ||
            lowerMessage.contains("coins!")) {
            return false;
        }
        return lowerMessage.contains("double hook") ||
               lowerMessage.contains("you've hooked an");
    }
    
    private static ParseResult tryPattern(DropPattern pattern, String cleanMessage) {
        Matcher matcher = pattern.pattern.matcher(cleanMessage);
        if (!matcher.find()) {
            return null;
        }
        
        try {
            ParseResult result = extractItemAndQuantity(pattern, matcher, cleanMessage);
            
            if (result != null && (result.isCoinDrop || isTrackableItem(result.itemName, cleanMessage))) {
                return result;
            }
        } catch (Exception e) {
            // Continue to next pattern if parsing fails
        }
        
        return null;
    }
    
    private static ParseResult extractItemAndQuantity(DropPattern pattern, Matcher matcher, String cleanMessage) {
        String itemName;
        int quantity;
        boolean isCoinDrop = false;
        
        try {
            if (pattern.quantityGroup == -1) {
                quantity = 1;
                itemName = matcher.group(pattern.itemGroup);
            } else if (pattern.quantityGroup == -2) {
                String quantityStr = matcher.group(1).replace(",", "");
                quantity = Integer.parseInt(quantityStr);
                itemName = COINS_KEYWORD;
                isCoinDrop = true;
            } else {
                String quantityStr = matcher.group(pattern.quantityGroup).replace(",", "");
                quantity = Integer.parseInt(quantityStr);
                itemName = matcher.group(pattern.itemGroup);
            }
            
            if (!isCoinDrop && isCoinsPattern(itemName)) {
                String coinMatch = itemName.replaceAll("[^\\d,]", "");
                if (!coinMatch.isEmpty()) {
                    quantity = Integer.parseInt(coinMatch.replace(",", ""));
                    itemName = COINS_KEYWORD;
                    isCoinDrop = true;
                }
            }
            
            itemName = handleSpecialMessages(itemName, cleanMessage);
            itemName = cleanItemName(itemName);
            
            if (isCoinDrop) {
                return new ParseResult(COINS_KEYWORD, quantity, true);
            }
            
            itemName = detectAndAddShardSuffix(itemName, cleanMessage);
            itemName = ensureShardSuffix(itemName, cleanMessage);
            String displayName = toSingular(itemName);

            return new ParseResult(displayName, quantity, false, null);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static boolean isCoinsPattern(String itemName) {
        return itemName.matches("[\\d,]+\\s+coins?!?") || 
               itemName.matches("\\d[\\d,]*\\s+coins?!?") ||
               itemName.toLowerCase().matches("[\\d,]+\\s+coins?!?");
    }

    // Check if an item should be treated as a shard
    private static String detectAndAddShardSuffix(String itemName, String originalMessage) {
        String lowerItem = itemName.toLowerCase().trim();
        String lowerMessage = originalMessage.toLowerCase();
        if (lowerItem.contains(SHARD_KEYWORD)) {
            return itemName;
        }
        
        if (lowerItem.equals(COINS_KEYWORD) || lowerItem.equals(COIN_KEYWORD) || lowerItem.contains("coin")) {
            return itemName;
        }
        
        if (TRACKED_SHARDS.contains(lowerItem) &&
            (lowerMessage.contains("caught") || lowerMessage.contains("good catch") || 
             lowerMessage.contains("great catch") || lowerMessage.contains("outstanding catch"))) {
            return itemName + " " + SHARD_KEYWORD;
        }
        
        return itemName;
    }
    
    private static String ensureShardSuffix(String itemName, String originalMessage) {
        String lowerMessage = originalMessage.toLowerCase();
        String lowerItem = itemName.toLowerCase();
        boolean isShardMessage = lowerMessage.contains(SHARD_KEYWORD) || 
                                lowerMessage.contains(CHARM_KEYWORD) || 
                                lowerMessage.contains(NAGA_KEYWORD) ||
                                lowerMessage.contains(SALT_YOU_CHARMED) ||
                                lowerMessage.contains(LOOT_SHARE);
        
        // If it's a shard message and the item doesn't already end with "shard"
        if (isShardMessage && !lowerItem.endsWith(SHARD_KEYWORD)
            && !lowerItem.equals(COINS_KEYWORD) && !lowerItem.equals(COIN_KEYWORD) && !lowerItem.contains("coin")) {
            return itemName + " " + SHARD_KEYWORD;
        }
        
        return itemName;
    }
    
    private static String handleSpecialMessages(String itemName, String cleanMessage) {
        String lowerMessage = cleanMessage.toLowerCase();
        if ((lowerMessage.startsWith(CHARM_KEYWORD) || lowerMessage.startsWith("salt")) || lowerMessage.startsWith(NAGA_KEYWORD) &&
            (lowerMessage.contains("captured") || lowerMessage.contains(SHARD_KEYWORD))) {
            return itemName + " " + SHARD_KEYWORD;
        }
        return itemName;
    }
    
    private static String toSingular(String itemName) {
        String normalized = itemName.toLowerCase();
        
        // If it's a shard, ensure singular form for display
        if (normalized.contains(SHARD_KEYWORD) && normalized.endsWith(SHARD_PLURAL)) {
            return itemName.substring(0, itemName.length() - 1);
        }
        
        return itemName;
    }
    
    private static String cleanItemName(String itemName) {
        // Check cache first
        String cachedResult = ApiCache.getCachedNormalization(itemName);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        String cleaned = itemName.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\"'`]", "")
                .replaceAll("\\([^)]*\\)", "")
                .replace("✯", "")
                .trim()
                .toLowerCase();
        
        String normalized = normalizePlural(cleaned);
        ApiCache.cacheNormalization(itemName, normalized);
        
        return normalized;
    }
    
    /**
     * Normalize plural item names to singular for consistent tracking
     */
    private static String normalizePlural(String itemName) {
        // Handle common shard plurals (most frequent case)
        if (itemName.endsWith(SHARD_PLURAL)) {
            return itemName.replace(SHARD_PLURAL, " " + SHARD_KEYWORD);
        }
        
        // Handle other specific plurals
        itemName = normalizeSpecificPlurals(itemName);
        
        // Handle generic plurals for known tracked items
        return normalizeKnownShardPlurals(itemName);
    }
    
    private static String normalizeSpecificPlurals(String itemName) {
        if (itemName.endsWith(COINS_KEYWORD)) {
            return itemName.replace(COINS_KEYWORD, COIN_KEYWORD);
        }
        
        return itemName;
    }
    
    private static String normalizeKnownShardPlurals(String itemName) {
        for (String shard : TRACKED_SHARDS) {
            if (itemName.equals(shard + "s")) {
                return shard;
            }
        }
        return itemName;
    }
    
    /**
     * Check if an item should be tracked
     */
    private static boolean isTrackableItem(String itemName, String cleanMessage) {
        String normalized = itemName.toLowerCase();
        String lowerMessage = cleanMessage.toLowerCase();
    
        if (isPlainCoin(normalized)) {
            return false;
        }
        
        if (TRACKED_SHARDS.contains(normalized) || normalized.contains(COINS_KEYWORD)) {
            return true;
        }
        
        if (isShardItem(normalized)) {
            return true;
        }
        
        if (isRareDropPattern(lowerMessage)) {
            return true;
        }
        
        return hasValuableKeyword(normalized);
    }
    
    private static boolean isRareDropPattern(String lowerMessage) {
        return lowerMessage.contains(RARE_DROP) ||
               lowerMessage.contains(VERY_RARE_DROP) ||
               lowerMessage.contains(CRAZY_RARE_DROP) ||
               lowerMessage.contains(EXTREMELY_RARE_DROP) ||
               lowerMessage.contains(INSANE_DROP) ||
               lowerMessage.contains(PRAY_TO_RNGESUS_DROP) ||
               lowerMessage.contains("rare catch") ||
               lowerMessage.contains("good catch") ||
               lowerMessage.contains("great catch") ||
               lowerMessage.contains("outstanding catch") ||
               lowerMessage.contains(PET_DROP) ||
               lowerMessage.contains(WOW_DUG_OUT);
    }
    
    private static boolean isPlainCoin(String normalized) {
        return normalized.equals(COINS_KEYWORD) || normalized.equals(COIN_KEYWORD);
    }
    
    private static boolean isShardItem(String normalized) {
        if (!normalized.contains(SHARD_KEYWORD)) {
            return false;
        }
        if (normalized.contains("raw salmon") || normalized.contains("shard of the shredded") || 
            normalized.contains("prismarine shard") || normalized.contains("earth shard")) {
            return false;
        }
        for (String shard : TRACKED_SHARDS) {
            if (normalized.contains(shard)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean hasValuableKeyword(String normalized) {
        for (String keyword : VALUABLE_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Data class for drop patterns
     */
    private static class DropPattern {
        final Pattern pattern;
        final int quantityGroup;
        final int itemGroup;
        
        DropPattern(Pattern pattern, int quantityGroup, int itemGroup) {
            this.pattern = pattern;
            this.quantityGroup = quantityGroup;
            this.itemGroup = itemGroup;
        }
    }
    
    /**
     * Result of parsing a chat message
     */
    public static class ParseResult {
        public final String itemName;
        public final int quantity;
        public final boolean isCoinDrop;
        public final String tooltipContent;
        
        public ParseResult(String itemName, int quantity) {
            this(itemName, quantity, false, null);
        }
        
        public ParseResult(String itemName, int quantity, boolean isCoinDrop) {
            this(itemName, quantity, isCoinDrop, null);
        }
        
        public ParseResult(String itemName, int quantity, boolean isCoinDrop, String tooltipContent) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.isCoinDrop = isCoinDrop;
            this.tooltipContent = tooltipContent;
        }
        
        @Override
        public String toString() {
            if (isCoinDrop) {
                return String.format("%s %s", String.format("%,d", quantity), itemName);
            }
            return String.format("%dx %s", quantity, itemName);
        }
    }

    private ChatDropParser() {
        throw new UnsupportedOperationException("Utility class");
    }
}
