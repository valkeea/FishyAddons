package me.valkeea.fishyaddons.util.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.text.Text;

public class ScoreboardUtils {
    private static String gameMode = null;

    private ScoreboardUtils() {}

    public static List<String> getSidebarLines() {
        var mc = MinecraftClient.getInstance();
        if (mc.world == null) return Collections.emptyList();

        var scoreboard = mc.world.getScoreboard();
        var objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return Collections.emptyList();

        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(objective));
        entries.sort((a, b) -> Integer.compare(b.value(), a.value()));

        List<String> lines = new ArrayList<>();

        for (ScoreboardEntry entry : entries) {
            var owner = entry.owner();
            var team = scoreboard.getScoreHolderTeam(owner);

            String line;
            if (team != null) {
                line = team.getPrefix().getString() + team.getSuffix().getString();
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
        var mc = MinecraftClient.getInstance();
        if (mc.world == null) return null;
        
        var scoreboard = mc.world.getScoreboard();
        var objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        Text titleText = objective != null ? objective.getDisplayName() : Text.empty();
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
