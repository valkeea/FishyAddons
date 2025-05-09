package me.wait.fishyaddons.util;

// import me.wait.fishyaddons.handlers.RetexHandler;

public class ZoneUtils {
    private static boolean isDungeons = false;

    private ZoneUtils() {}

    private static void setDungeon() {
        String area = ScoreboardUtils.getLine(6);
        if (area != null) {
            area = area.trim();
            area = area.replaceAll("[^a-zA-Z0-9\\s]", ""); // Funny unicode.
            if (area.matches(".*The Catacombs.*")) {
                System.out.println("Detected dungeons area: " + area);
                isDungeons = true;
                AreaUtils.setIsland("dungeon");
            } else {
                System.out.println("Detected non-dungeons area: " + area);
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
