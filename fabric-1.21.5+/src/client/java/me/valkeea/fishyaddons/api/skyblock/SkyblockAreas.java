package me.valkeea.fishyaddons.api.skyblock;

import me.valkeea.fishyaddons.feature.skyblock.TransLava;
import me.valkeea.fishyaddons.tracker.fishing.ScStats;

/**
 * This approach allows for instant area detection based on spawn position.
 * Failure is possible but rare and for critical features a fallback can be implemented.
 **/
public class SkyblockAreas {
    private SkyblockAreas() {}

    public enum Island {
        CI("crimson_isle"),
        HUB("hub"),
        PRIVATE("private_island"),
        DH("dungeon_hub"),
        CH("crystal_hollows"),
        DEEP("deep_caverns"),
        GOLD_MINE("gold_mine"),
        DM("dwarven_mines"),
        END("the_end"),
        FI("the_farming_islands"),
        GT("glacite_tunnels"),
        PARK("the_park"),
        DEN("spiders_den"),
        DEF("default"),
        GAL("galatea"),
        BAYOU("bayou"),
        JERRY("jerrys_workshop"),
        DUNGEON("dungeon"),
        DG_HUB("dungeon_hub"),
        MINESHAFT("mineshaft"),
        PLHLEGBLAST("crimson_plhleg"),
        CI_HOTSPOT("crimson_hotspot"),
        NA("unknown");

        private final String name;

        Island(String name) {
            this.name = name;
        }

        public String key() {
            return name;
        }

        public String displayName() {

            String[] words = name.split("_");
            StringBuilder displayName = new StringBuilder();

            for (String word : words) {
                displayName.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
            return displayName.toString().trim();
        }

    }

    private static Island currentIsland = Island.NA;
    private static boolean isGalatea = false;
    private static boolean isCrimson = false;  

    public static Island getIsland() {
        return currentIsland;
    }

    public static void setIsland(Island island) {
        currentIsland = island;
        isGalatea = Island.GAL.equals(island);
        checkCrimson(island);
        ScStats.setArea(island);
    }

    public static void setIslandByMap(String mapName) {
        String normalized = mapName.toLowerCase().replace(" ", "_").replaceAll("[^a-z0-9_]", "");
        for (Island island : Island.values()) {
            if (island.key().equals(normalized)) {
                setIsland(island);
                return;
            }
        }
        setIsland(Island.NA);
    }

    public static boolean isCrimson() {
        return isCrimson;
    }

    public static void checkCrimson(Island island) {
        isCrimson = Island.CI.equals(island);
        TransLava.update();
    }

    public static void updateCi() {
        if (!isCrimson) {
            TransLava.update();
            ScStats.setArea(Island.CI);
        }
        isCrimson = true;
    }

    public static boolean isGalatea() {
        return isGalatea;
    }

    public static boolean isDenOrPark() {
        return Island.DEN.equals(currentIsland) || Island.PARK.equals(currentIsland);
    }
}