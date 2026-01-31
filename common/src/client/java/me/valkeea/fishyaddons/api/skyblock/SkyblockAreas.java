package me.valkeea.fishyaddons.api.skyblock;

import me.valkeea.fishyaddons.event.impl.EnvironmentChangeEvent;
import me.valkeea.fishyaddons.event.impl.FaEvents;

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
        RIFT("the_rift"),
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

    private static volatile boolean checked = false;
    private static volatile Island currentIsland = Island.NA;
    private static final Object lock = new Object();

    public static void reset() {
        synchronized (lock) {
            checked = false;
            currentIsland = Island.NA;
        }
    }

    public static Island getIsland() {
        return currentIsland;
    }

    public static void setIsland(Island island) {
        synchronized (lock) {
            if (checked) return;
            checked = true;
            currentIsland = island;
            var event = new EnvironmentChangeEvent(currentIsland);
            FaEvents.ENVIRONMENT_CHANGE.firePhased(event, listener -> listener.onEnvironmentChange(event));
        }
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

    public static void setIslandFromSidebar(Island island) {
        synchronized (lock) {
            if (currentIsland.equals(island) && checked) return;
            checked = true;
            currentIsland = island;
            var event = new EnvironmentChangeEvent(currentIsland);
            FaEvents.ENVIRONMENT_CHANGE.firePhased(event, listener -> listener.onEnvironmentChange(event));
        }
    }

    public static boolean isCrimson() {
        return currentIsland == Island.CI;
    }

    public static boolean isGalatea() {
        return currentIsland == Island.GAL;
    }

    public static boolean isDenOrPark() {
        return Island.DEN.equals(currentIsland) || Island.PARK.equals(currentIsland);
    }
}
