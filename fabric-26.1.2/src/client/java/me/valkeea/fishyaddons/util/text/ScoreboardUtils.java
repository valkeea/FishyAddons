package me.valkeea.fishyaddons.util.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerScoreEntry;

public class ScoreboardUtils {
    private static String gameMode = null;

    private ScoreboardUtils() {}

    public static List<String> getSidebarLines() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return Collections.emptyList();

        var scoreboard = mc.level.getScoreboard();
        var objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return Collections.emptyList();

        List<PlayerScoreEntry> entries = new ArrayList<>(scoreboard.listPlayerScores(objective));
        entries.sort((a, b) -> Integer.compare(b.value(), a.value()));

        List<String> lines = new ArrayList<>();

        for (PlayerScoreEntry entry : entries) {
            var owner = entry.owner();
            var team = scoreboard.getPlayersTeam(owner);

            String line;
            if (team != null) {
                line = team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
            } else line = owner;

            lines.add(line);
        }

        return lines;
    }

    public static String getLine(int index) {
        List<String> lines = getSidebarLines();
        if (index < 0 || index >= lines.size()) return null;
        return stripColor(lines.get(index));
    }

    public static String getGamemode() {
        if (gameMode == null) {
            getSidebarLines();
        }
        return gameMode;
    }

    public static String getSidebarObjectiveName() {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        
        var scoreboard = mc.level.getScoreboard();
        var objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        Component titleText = objective != null ? objective.getDisplayName() : Component.empty();
        String title = titleText.getString();

        return title.isEmpty() ? null : title;
    }

    public static String stripColor(String input) {
        return input == null ? null : input.replaceAll("§[0-9a-fk-or]", "");
    }

    public static void logSidebar() {
        List<String> sidebar = getSidebarLines();
        for (String line : sidebar) {
            System.out.println("[Sidebar] --- " + line);
        }
    }
}
