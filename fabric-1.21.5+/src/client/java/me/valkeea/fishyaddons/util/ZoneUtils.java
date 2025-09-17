package me.valkeea.fishyaddons.util;

import me.valkeea.fishyaddons.handler.WeatherTracker;
import me.valkeea.fishyaddons.listener.WorldEvent;
import me.valkeea.fishyaddons.util.text.ScoreboardUtils;
import java.util.Arrays;

public class ZoneUtils {
    private ZoneUtils() {}
    private static boolean isDungeons = false;
    private static boolean denOrPark = false;

    private static void setDungeon() {
        StringBuilder areaBuilder = new StringBuilder();
        String l10 = ScoreboardUtils.getLine(9);
        String l9 = ScoreboardUtils.getLine(8);
        String l8 = ScoreboardUtils.getLine(7);
        String l7 = ScoreboardUtils.getLine(6);
        String l6 = ScoreboardUtils.getLine(5);
        String l5 = ScoreboardUtils.getLine(4);

        for (String line : Arrays.asList(l5, l6, l7, l8, l9, l10)) {
            if (line != null) areaBuilder.append(line).append(" ");
        }

        String area = areaBuilder.toString().trim();

        if (!area.isEmpty()) {
            area = area.replaceAll("[^a-zA-Z0-9\\s]", "");
            boolean hasCrimson = area.contains("Crimson Isle");
            boolean hasCatacombs = area.contains("The Catacombs");
            boolean hasTimeElapsed = area.contains("Time Elapsed");

            if (hasCrimson) {
                AreaUtils.updateCi();
            }
            if (hasCatacombs && hasTimeElapsed) {
                isDungeons = true;
                AreaUtils.setIsland("dungeon");
                WorldEvent.getInstance().reset();
            } else if (hasCatacombs) {
                isDungeons = false;
                WorldEvent.getInstance().reCheck(80);
            } else {
                isDungeons = false;
            }
        }
    }

    public static boolean checkDenOrPark() {
        if (AreaUtils.isDenOrPark() || denOrPark) {
            return true;
        }
        ScoreboardUtils.getSidebarLines().forEach(line -> {
            if (line != null && (line.contains("The Park") || line.contains("Birch Park"))) {
                AreaUtils.setIsland("park");
                WeatherTracker.track();
                denOrPark = true;
                return;
            } else if (line != null && line.contains("Spider's Den")) {
                AreaUtils.setIsland("den");
                WeatherTracker.track();
                denOrPark = true;
                return;
            }
        });
        denOrPark = false;
        return false;
    }

    public static boolean isInDungeon() {
        return isDungeons;
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