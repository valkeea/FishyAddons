package me.valkeea.fishyaddons.util;

import java.util.List;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.feature.skyblock.WeatherTracker;
import me.valkeea.fishyaddons.listener.WorldEvent;
import me.valkeea.fishyaddons.util.text.ScoreboardUtils;
import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.client.Minecraft;

public class ZoneUtils {
    private ZoneUtils() {}
    private static boolean isDungeons = false;
    private static boolean isLobby = false;
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

            } else {
                isDungeons = false;
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
                SkyblockAreas.setIslandFromSidebar(area);
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

    public static void resetRain() {
        rainArea = false;
    }

    public static boolean activeLocQuery() {
        boolean wasActive = locQuery;
        locQuery = false;
        return wasActive;
    }
}
