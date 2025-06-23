package me.valkeea.fishyaddons.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoreboardUtils {
    private static String gameMode = null;

    private ScoreboardUtils() {}

    public static List<String> getSidebarLines() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return Collections.emptyList();
        }

        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return Collections.emptyList();
        }

        List<ScoreboardEntry> entries = new ArrayList<>(scoreboard.getScoreboardEntries(sidebar));
        // Sort by entry.value() descending (highest score at the top)
        entries.sort((a, b) -> Integer.compare(b.value(), a.value()));
        List<String> lines = new ArrayList<>();

        for (ScoreboardEntry entry : entries) {
            String owner = entry.owner();
            Team team = scoreboard.getScoreHolderTeam(owner);
            String line;
            if (team != null) {
                line = team.getPrefix().getString() + team.getSuffix().getString();
            } else {
                line = owner;
            }
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
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return null;
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective sidebar = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        // Get the sidebar title as a plain string
        Text titleText = sidebar != null ? sidebar.getDisplayName() : Text.empty();
        String title = titleText.getString();
        return title.isEmpty() ? null : title;
    }

    // Simple color strip
    public static String stripColor(String input) {
        return input == null ? null : input.replaceAll("§[0-9a-fk-or]", "");
    }

    // For debugging purposes
    public static void logSidebar() {
        List<String> sidebar = getSidebarLines();
        for (String line : sidebar) {
            System.out.println("[Sidebar] --- " + line);
        }
    }
}
