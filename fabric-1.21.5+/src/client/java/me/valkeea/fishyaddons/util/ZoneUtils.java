package me.valkeea.fishyaddons.util;

import me.valkeea.fishyaddons.listener.WorldEvent;

public class ZoneUtils {
    private static boolean isDungeons = false;

    private ZoneUtils() {}

    private static void setDungeon() {
        StringBuilder areaBuilder = new StringBuilder();
        String line10 = ScoreboardUtils.getLine(9);
        String line9 = ScoreboardUtils.getLine(8);
        String line8 = ScoreboardUtils.getLine(7);
        String line7 = ScoreboardUtils.getLine(6);
        String line6 = ScoreboardUtils.getLine(5);
        String line5 = ScoreboardUtils.getLine(4);

        if (line5 != null) areaBuilder.append(line5).append(" ");
        if (line6 != null) areaBuilder.append(line6).append(" ");
        if (line7 != null) areaBuilder.append(line7).append(" ");
        if (line8 != null) areaBuilder.append(line8).append(" ");
        if (line9 != null) areaBuilder.append(line9).append(" ");
        if (line10 != null) areaBuilder.append(line10);

        String area = areaBuilder.toString().trim();
        if (!area.isEmpty()) {
            area = area.replaceAll("[^a-zA-Z0-9\\s]", ""); // Remove funny unicode.
            boolean hasCatacombs = area.contains("The Catacombs");
            boolean hasTimeElapsed = area.contains("Time Elapsed");
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

    public static boolean isInDungeon() {
        return isDungeons;
    }

    public static void update() {
        setDungeon();
    }
}