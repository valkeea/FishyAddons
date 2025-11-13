package me.valkeea.fishyaddons.util;

import java.util.List;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.feature.skyblock.WeatherTracker;
import me.valkeea.fishyaddons.listener.WorldEvent;
import me.valkeea.fishyaddons.util.text.ScoreboardUtils;

public class ZoneUtils {
    private ZoneUtils() {}
    private static boolean isDungeons = false;
    private static boolean isLobby = false;
    private static boolean denOrPark = false;

    private static final List<String> ciIndicators = List.of(
        "The Wasteland",
        "Forgotten Skull",
        "Stronghold",
        "Blazing Volcano",
        "Mystic Marsh",
        "Crimson Isle",
        "Crimson Fields",
        "Burning Desert",
        "Smoldering Tomb"
    );

    private static void setDungeon() {
        isLobby = false;

        var areaBuilder = new StringBuilder();
        for (String line : ScoreboardUtils.getSidebarLines()) {
            if (line != null && !line.isEmpty()) {
                areaBuilder.append(line).append(" ");
            }
        }

        String area = areaBuilder.toString().trim();

        if (!area.isEmpty()) {
            area = area.replaceAll("[^a-zA-Z0-9\\s]", "");
            boolean hasCrimson = ciIndicators.stream().anyMatch(area::contains);
            boolean hasCatacombs = area.contains("The Catacombs");
            boolean hasTimeElapsed = area.contains("Time Elapsed");

            if (hasCrimson) SkyblockAreas.setIslandFromSidebar(Island.CI);

            if (hasCatacombs && hasTimeElapsed) {
                isDungeons = true;
                SkyblockAreas.setIslandFromSidebar(Island.DUNGEON);
                WorldEvent.getInstance().reset();
                
            } else if (hasCatacombs) {
                isDungeons = false;
                isLobby = true;
                WorldEvent.getInstance().reCheck(60);

            } else isDungeons = false;
        }
    }

    public static boolean checkDenOrPark() {
        if (denOrPark || SkyblockAreas.isDenOrPark()) {
            return true;
        }

        for (String line : ScoreboardUtils.getSidebarLines()) {
            if (line != null && (line.contains("The Park") || line.contains("Birch Park"))) {
                SkyblockAreas.setIslandFromSidebar(Island.PARK);
                WeatherTracker.track();
                denOrPark = true;
                return true;

            } else if (line != null && line.contains("Spiders Den")) {
                SkyblockAreas.setIslandFromSidebar(Island.DEN);
                WeatherTracker.track();
                denOrPark = true;
                return true;
            }
        }

        denOrPark = false;
        return false;
    }

    public static boolean isInDungeon() {
        return isDungeons;
    }

    public static boolean isDungeonInstance() {
        return isDungeons || isLobby;
    }

    public static void resetDungeon() {
        if (isDungeons) {
            isDungeons = false;
        }
    }

    public static void update() {
        setDungeon();
    }

    public static void resetRain() {
        denOrPark = false;
    }    
}
