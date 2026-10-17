package me.valkeea.fishyaddons.util;

import java.util.List;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.feature.skyblock.WeatherTracker;
import me.valkeea.fishyaddons.util.text.ScoreboardUtils;
import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.client.Minecraft;

public class ZoneUtils {
    private ZoneUtils() {}
    private static boolean isDungeonRun = false;
    private static boolean rainArea = false;
    private static boolean locQuery = false;

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

    public static void update() {

        var areaBuilder = new StringBuilder();
        for (String l : ScoreboardUtils.getSidebarLines()) {
            if (l != null && !l.isEmpty()) {
                areaBuilder.append(l).append(" ");
            }
        }

        String area = areaBuilder.toString().trim();

        if (!area.isEmpty()) {
            area = area.replaceAll("[^a-zA-Z0-9\\s]", "");
            boolean hasCrimson = ciIndicators.stream().anyMatch(area::contains);
            if (hasCrimson) {
                SkyblockAreas.setIslandDelayed(Island.CI);
            }
        }

        verifyLocation();
    }

    public static boolean checkRainArea() {
        if (rainArea || SkyblockAreas.isRainArea()) {
            return true;
        }

        for (String i : ScoreboardUtils.getSidebarLines()) {
            if (i == null || !i.contains("⏣")) continue;
            var l = TextUtils.stripColor(i).strip();
            Island area = parseArea(l);
            if (area != null) {
                SkyblockAreas.setIslandDelayed(area);
                WeatherTracker.track();
                rainArea = true;
                return true;
            }
        }

        rainArea = false;
        return false;
    }

    private static Island parseArea(String line) {
        return switch (line) {
            case "Birch Park" -> Island.PARK;
            case "Spiders Den" -> Island.DEN;
            case "Lotus Atoll" -> Island.LOTUS;
            case "Backwater Bayou" -> Island.BAYOU;
            default -> null;
        };
    }

    private static void verifyLocation() {
        if (!(GameMode.onHypixel() && SkyblockAreas.getIsland() == Island.NA)) return;
        var conn = Minecraft.getInstance().player.connection;
        if (conn != null) {
            conn.sendCommand("locraw");
            locQuery = true;
        }
    }

    public static void startedDungeon() {
        SkyblockAreas.setIslandDelayed(Island.DUNGEON);
        isDungeonRun = true;
    }

    public static boolean isInDungeon() {
        return isDungeonRun;
    }

    public static boolean isDungeonInstance() {
        return SkyblockAreas.getIsland() == Island.DUNGEON;
    }

    public static void resetDungeon() {
        if (isDungeonRun) {
            isDungeonRun = false;
        }
    }

    public static void resetRain() {
        rainArea = false;
    }

    public static boolean activeLocQuery() {
        boolean wasActive = locQuery;
        locQuery = false;
        return wasActive;
    }
}
