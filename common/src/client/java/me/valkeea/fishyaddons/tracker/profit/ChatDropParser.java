package me.valkeea.fishyaddons.tracker.profit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

public class ChatDropParser {
    private static final String SHARD_KW = "shard";
    private static final String SHARD_PLURAL = " shards";
    private static final String COINS_KW = "coins";
    private static final String COIN_KW = "coin";
    private static final String ANY_CATCH = "⛃\\s+\\w+\\s+catch!\\s+you caught\\s+";
    private static final String DROP_REGEX_SUFFIX = "!\\s*([^(\\r\\n]+?)(?:\\s*\\([^)]*\\).*)?$";
    
    private static final List<DropPattern> DROP_PATTERNS = new ArrayList<>();
    private static final Set<String> TRACKED_SHARDS = new HashSet<>();
    private static final Set<String> VALUABLE_KEYWORDS = new HashSet<>();
    
    enum DropKeyword {
        
        // Tier keywords
        RARE_DROP("rare drop", true),
        VERY_RARE_DROP("very rare drop", true),
        CRAZY_RARE_DROP("crazy rare drop", true),
        EXTREMELY_RARE_DROP("extremely rare drop", true),
        INSANE_DROP("insane drop", true),
        PRAY_TO_RNGESUS_DROP("pray to rngesus drop", true),
        PET_DROP("pet drop", true),
        
        // Other drop indicators
        CATCH("catch!", false),
        WOW_DUG_OUT("wow! you dug out", false),
        YOU_CAUGHT("you caught", false),
        CHARM("charm", false),
        NAGA("naga", false),
        SALT_YOU_CHARMED("salt you charmed", false),
        LOOT_SHARE("loot share", false);
        
        final String keyword;
        final boolean isDropTier;
        
        DropKeyword(String keyword, boolean isDropTier) {
            this.keyword = keyword;
            this.isDropTier = isDropTier;
        }
        
        /**
         * Check if the message contains any drop keyword
         */
        static boolean containsAny(String message) {
            for (DropKeyword kw : values()) {
                if (message.contains(kw.keyword)) return true;
            }
            return false;
        }
        
        /**
         * Build regex pattern for drop tiers
         */
        static String buildDropTierRegex(boolean includePetDrop) {
            StringBuilder sb = new StringBuilder("(?:");
            boolean first = true;
            for (DropKeyword kw : values()) {
                if (kw.isDropTier && (includePetDrop || kw != PET_DROP)) {
                    if (!first) sb.append("|");
                    sb.append(kw.keyword);
                    first = false;
                }
            }
            sb.append(")");
            return sb.toString();
        }
    }
    
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
            "book", "talisman", "[Lvl 1]", COIN_KW, SHARD_KW
        };
        
        for (String kw : keywords) {
            VALUABLE_KEYWORDS.add(kw.toLowerCase());
        }
    }
    
    private static void initPatterns() {
        
        String rareDrops = DropKeyword.buildDropTierRegex(false);
        String allDrops = DropKeyword.buildDropTierRegex(true);
        
        // Pattern 1: "RARE DROP! You dug out ItemName!"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(DropKeyword.RARE_DROP.keyword + "!\\s*You dug out an? ([^!]+)!", Pattern.CASE_INSENSITIVE),
            DropType.ITEM, -1, 1
        ));

        // Pattern 2: X DROP! Enchanted Book (itemName) (+X ✯ Magic Find)
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(rareDrops + "!\\s*Enchanted Book\\s*\\(([^)]+?)\\)(?:\\s*\\([^)]*\\))?.*?$", Pattern.CASE_INSENSITIVE),
            DropType.BOOK, -1, 1
        ));

        // Pattern 3a: "X DROP! (31x ItemName) (+Magic Find)"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(rareDrops + "!\\s*\\((\\d+)x\\s+([^)]+?)\\)(?:\\s*\\([^)]*\\))?.*?$", Pattern.CASE_INSENSITIVE),
            DropType.ITEM, 1, 2
        ));

        // Pattern 3b: "X DROP! (ItemName) (+Magic Find)"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(rareDrops + "!\\s*\\((?!\\d+x\\s)([^)]+?)\\)(?:\\s*\\([^)]*\\))?.*?$", Pattern.CASE_INSENSITIVE),
            DropType.ITEM, -1, 1
        ));

        // Pattern 4: "X DROP! ItemName"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(allDrops + DROP_REGEX_SUFFIX, Pattern.CASE_INSENSITIVE),
            DropType.ITEM, -1, 1
        ));
        
        // Pattern 5a: "⛃ <tier> CATCH! You caught ItemName xX!"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(ANY_CATCH + "(.+?)\\s+x(\\d{1,3})!", Pattern.CASE_INSENSITIVE),
            DropType.ITEM, 2, 1
        ));

        // Pattern 5b: "⛃ <tier> CATCH! You caught a/an ItemName!"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(ANY_CATCH + "(?:an?\\s+)?(.+?)!", Pattern.CASE_INSENSITIVE),
            DropType.ITEM, -1, 1
        ));
        
        // Pattern 6: "Wow! You dug out X coins!"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile(DropKeyword.WOW_DUG_OUT.keyword + " ([\\d,]+)\\s+coins!", Pattern.CASE_INSENSITIVE),
            DropType.COIN, 1, -1
        ));
        
        // Pattern 7a: "You caught xX ItemName Shards!"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile("you caught x(\\d+)\\s+(.+?)\\s*shards?!", Pattern.CASE_INSENSITIVE),
            DropType.SHARD, 1, 2
        ));
        
        // Pattern 7b: "You caught a/an ItemName Shard!"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile("you caught an?\\s+(.+?)\\s*shard!", Pattern.CASE_INSENSITIVE),
            DropType.SHARD, -1, 1
        ));
        
        // Pattern 8a: "CHARM/SALT/NAGA You charmed a CreatureName and captured X Shards" (with or without "from it.")
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile("(?:salt|charm|naga)\\s+you charmed an?\\s+(.+?)\\s+and captured (\\d+)\\s+shards?", Pattern.CASE_INSENSITIVE),
            DropType.SHARD, 2, 1
        ));
        
        // Pattern 8b: "CHARM/SALT/NAGA You charmed a CreatureName and captured its Shard."
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile("(?:salt|charm|naga)\\s+you charmed an?\\s+(.+?)\\s+and captured its shard.", Pattern.CASE_INSENSITIVE),
            DropType.SHARD, -1, 1
        ));
        
        // Pattern 9: "LOOT SHARE You received a ItemName Shard for assisting <someone>!"
        DROP_PATTERNS.add(new DropPattern(
            Pattern.compile("loot share\\s+you received an?\\s+(.+?)\\s*shard for assisting\\s+\\w+!", Pattern.CASE_INSENSITIVE),
            DropType.SHARD, -1, 1
        ));
    }

    /**
     * Parse a chat message for drop information
     */
    public static @Nullable ParseResult parseMessage(String message) {
        if (message == null || message.trim().isEmpty()) return null;
        if (shouldIgnore(message)) return null;
        if (!DropKeyword.containsAny(message.toLowerCase())) return null;
        
        for (var pattern : DROP_PATTERNS) {
            Matcher m = pattern.pattern.matcher(message);
            if (!m.find()) continue;
            
            ParseResult r = tryPattern(pattern, m, message);
            if (r != null) return r;
        }

        return null;
    }

    private static ParseResult tryPattern(DropPattern pattern, Matcher m, String message) {
        try {

            var rawItemName = extractItemName(pattern, m);
            int quantity = extractQuantity(pattern, m);
            var type = pattern.type;
            
            if (type.equals(DropType.COIN)) {
                return new ParseResult(COINS_KW, quantity, DropType.COIN);
            }
            
            if (rawItemName != null && isCoinsPattern(rawItemName)) {
                var coinTest = rawItemName.replaceAll("[^\\d,]", "");
                if (!coinTest.isEmpty()) {
                    int coinQuantity = Integer.parseInt(coinTest.replace(",", ""));
                    return new ParseResult(COINS_KW, coinQuantity, DropType.COIN);
                }
            }
            
            var normalized = normalize(rawItemName, type, message);
            if (isTrackable(normalized, message)) {
                return new ParseResult(normalized, quantity, type);
            }

            return null;

        } catch (Exception e) {
            return null; // Parsing failed for this pattern
        }
    }

    private static String extractItemName(DropPattern pattern, Matcher m) {
        if (pattern.itemGroup == -1) return null;
        return m.group(pattern.itemGroup);
    }

    private static int extractQuantity(DropPattern pattern, Matcher m) {
        if (pattern.quantityGroup == -1) return 1;
        String quantityStr = m.group(pattern.quantityGroup).replace(",", "");
        return Integer.parseInt(quantityStr);
    }

    /**
     * Normalize item name based on type and content
     */
    private static String normalize(String rawS, DropType type, String originalS) {
        if (rawS == null) return null;
        
        String s = rawS.trim()
            .replaceAll("\\s+", " ")
            .replaceAll("[\"'`]", "")
            .replaceAll("\\([^)]*\\)", "")
            .replace("✯", "")
            .trim()
            .toLowerCase();
        
        if (type.equals(DropType.SHARD)) {
            if (!s.endsWith(SHARD_KW)) s = s.concat(" " + SHARD_KW);
            if (s.endsWith(SHARD_PLURAL)) s = s.replace(SHARD_PLURAL, " " + SHARD_KW);

        } else {
            String lowerS = originalS.toLowerCase();
            if (TRACKED_SHARDS.contains(s) && 
                (lowerS.contains("caught") || lowerS.contains("catch"))) {
                s = s.concat(" " + SHARD_KW);
            }
        }
        
        if (isCoinsPattern(s)) {
            return COINS_KW;
        }
        
        return s;
    }

    /**
     * Check if item should be tracked
     */
    private static boolean isTrackable(String normalized, String original) {
        if (normalized == null) return false;
        
        if (normalized.equals(COINS_KW) || normalized.equals(COIN_KW)) {
            return false; // Ignore plain coin references
        }
        
        if (normalized.contains(COINS_KW)) return true;
        if (isShardItem(normalized)) return true;
        
        // Track items from rare drop messages
        String lower = original.toLowerCase();
        for (DropKeyword kw : DropKeyword.values()) {
            if (kw.isDropTier && lower.contains(kw.keyword)) {
                return true;
            }
        }
        
        // Check for catch patterns
        if (lower.contains("rare catch") || 
            lower.contains("good catch") ||
            lower.contains("great catch") || 
            lower.contains("outstanding catch") ||
            lower.contains(DropKeyword.WOW_DUG_OUT.keyword)) {
            return true;
        }
        
        // Track items with valuable keywords
        return hasValuableKeyword(normalized);
    }

    private static boolean shouldIgnore(String message) {
        String lower = message.toLowerCase();
        
        if (lower.contains(DropKeyword.CATCH.keyword) ||
            lower.contains(SHARD_KW) ||
            lower.contains("coins!")) {
            return false;
        }
        
        // Ignore fishing messages
        return lower.contains("double hook") || lower.contains("you've hooked an");
    }

    private static boolean isCoinsPattern(String itemName) {
        String lower = itemName.toLowerCase();
        return lower.matches("[\\d,]+\\s+coins?!?") || 
               lower.matches("\\d[\\d,]*\\s+coins?!?");
    }
    
    private static boolean isShardItem(String normalized) {
        if (!normalized.contains(SHARD_KW)) {
            return false;
        }
        
        if (normalized.contains("raw salmon") || 
            normalized.contains("shard of the shredded") || 
            normalized.contains("prismarine shard") || 
            normalized.contains("earth shard")) {
            return false; // Fake shards
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

    private static class DropPattern {
        final Pattern pattern;
        final DropType type;
        final int quantityGroup;  // -1 = quantity is 1
        final int itemGroup;      // -1 = no item (coins)
        
        DropPattern(Pattern pattern, DropType type, int quantityGroup, int itemGroup) {
            this.pattern = pattern;
            this.type = type;
            this.quantityGroup = quantityGroup;
            this.itemGroup = itemGroup;
        }
    }
    
    public static class ParseResult {
        public final String itemName;
        public final int quantity;
        public final DropType type;

        public ParseResult(String itemName, int quantity) {
            this(itemName, quantity, DropType.ITEM);
        }
        
        public ParseResult(String itemName, int quantity, DropType dropType) {
            this.itemName = itemName;
            this.quantity = quantity;
            this.type = dropType;
        }

        public boolean coinDrop() {
            return type.equals(DropType.COIN);
        }

        public boolean bookDrop() {
            return type.equals(DropType.BOOK);
        }
        
        @Override
        public String toString() {
            if (type.equals(DropType.COIN)) {
                return String.format("%s %s", String.format("%,d", quantity), itemName);
            }
            return String.format("%s x%d", itemName, quantity);
        }
    }

    public enum DropType {
        BOOK, COIN, SHARD, ITEM
    }

    private ChatDropParser() {
        throw new UnsupportedOperationException("Utility class");
    }
}
