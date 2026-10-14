package me.valkeea.fishyaddons.api.skyblock;

import me.valkeea.fishyaddons.util.text.StringUtils;
import net.minecraft.network.chat.Component;

public class SafariData {
    public enum Critter {
        CAVERNFISH(SafariZone.CAVERN, Rarity.COMMON),
        FLITTER(SafariZone.CAVERN, Rarity.COMMON),        
        SHYWORM(SafariZone.CAVERN, Rarity.UNCOMMON),
        DRIFTLING(SafariZone.CAVERN, Rarity.UNCOMMON),
        CHUCKWALLA(SafariZone.CAVERN, Rarity.RARE),
        ROCKMITE(SafariZone.CAVERN, Rarity.RARE),
        SCRAPPY(SafariZone.CAVERN, Rarity.RARE),
        SNOOZLE(SafariZone.CAVERN, Rarity.RARE),
        GEMZIE(SafariZone.CAVERN, Rarity.EPIC),
        FOXTROT(SafariZone.FOREST, Rarity.COMMON),
        BLUEBIRD(SafariZone.FOREST, Rarity.UNCOMMON),
        HONEYBUG(SafariZone.FOREST, Rarity.UNCOMMON),
        TREEFROG(SafariZone.FOREST, Rarity.UNCOMMON),
        WOODCHUCKER(SafariZone.FOREST, Rarity.UNCOMMON),
        FLUFFLING(SafariZone.FOREST, Rarity.RARE),
        HIDEONFLOOR(SafariZone.FOREST, Rarity.RARE),
        PARAKEET(SafariZone.FOREST, Rarity.RARE),
        MACAW(SafariZone.FOREST, Rarity.LEGENDARY),
        AREITA(SafariZone.HAUNTED, Rarity.UNCOMMON),
        BLOODBAT(SafariZone.HAUNTED, Rarity.UNCOMMON),
        DUPLICO(SafariZone.HAUNTED, Rarity.UNCOMMON),
        GAZER(SafariZone.HAUNTED, Rarity.UNCOMMON),
        LITTERBUG(SafariZone.HAUNTED, Rarity.UNCOMMON),
        SOLSNATCHER(SafariZone.HAUNTED, Rarity.UNCOMMON),
        GIMMIEGOLD(SafariZone.HAUNTED, Rarity.RARE),
        HIDEONWALL(SafariZone.HAUNTED, Rarity.RARE),
        HIDEYHO(SafariZone.HAUNTED, Rarity.RARE),
        DOOMSPIRAL(SafariZone.HAUNTED, Rarity.LEGENDARY),
        STRONGARM(SafariZone.ICY, Rarity.COMMON),
        TEPID(SafariZone.ICY, Rarity.COMMON),
        POLARIS(SafariZone.ICY, Rarity.UNCOMMON),
        SHUDDERSQUID(SafariZone.ICY, Rarity.UNCOMMON),
        BILLYGOAT(SafariZone.ICY, Rarity.RARE),
        MANTIS_SHRIMP(SafariZone.ICY, Rarity.RARE),
        NOZZLENOSE(SafariZone.ICY, Rarity.RARE),
        TROODON(SafariZone.ICY, Rarity.RARE),
        WUMPA(SafariZone.ICY, Rarity.LEGENDARY);

        private final SafariZone zone;
        private final Rarity rarity;
        private int count;
        private int shardCount;

        Critter(SafariZone zone, Rarity rarity) {
            this.zone = zone;
            this.rarity = rarity;
            this.count = 0;
            this.shardCount = 0;
        }

        @Override 
        public String toString() {
            return name().toLowerCase().replace('_', ' ');
        }

        public SafariZone getZone() {
            return zone;
        }

        public Rarity getRarity() {
            return rarity;
        }

        public Component getDisplayName() {
            var color = this.count > 0 ? rarity.getColorCode() : "§c";
            return Component.literal(color + StringUtils.capitalize(toString()));
        }
        
        public int getCaptureCount() {
            return count;
        }

        public int getShardCount() {
            return shardCount;
        }

        public void incrementCaptureCount() {
            this.count++;
        }

        public void addShardCount(int amount) {
            this.shardCount += amount;
        }

        public void resetStats() {
            this.count = 0;
            this.shardCount = 0;
        }
    }

    public enum SafariZone { CAVERN, FOREST, HAUNTED, ICY }
}
