package me.wait.fishyaddons.util;

import me.wait.fishyaddons.handlers.RetexHandler;
import me.wait.fishyaddons.listener.WorldEventListener;

public class ZoneUtils {
    private static boolean isDungeons = false;

    private ZoneUtils() {}

    private static void setDungeon() {
        StringBuilder areaBuilder = new StringBuilder();
        String line9 = ScoreboardUtils.getLine(8);
        String line8 = ScoreboardUtils.getLine(7);
        String line7 = ScoreboardUtils.getLine(6);
        String line6 = ScoreboardUtils.getLine(5);
        if (line6 != null) areaBuilder.append(line6).append(" ");
        if (line7 != null) areaBuilder.append(line7).append(" ");        
        if (line8 != null) areaBuilder.append(line8).append(" ");
        if (line9 != null) areaBuilder.append(line9);
        String area = areaBuilder.toString().trim();
        if (!area.isEmpty()) {
            area = area.replaceAll("[^a-zA-Z0-9\\s]", ""); // Funny unicode.
            if (area.matches(".*The Catacombs.*") && area.matches(".*Time Elapsed.*")) {
                isDungeons = true;
                AreaUtils.setIsland("dungeon");
                WorldEventListener.getInstance().reset();
            } else if (area.matches(".*The Catacombs.*") && !area.matches(".*Time Elapsed.*")) {
                isDungeons = false;
                WorldEventListener.getInstance().reCheck(80);
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
