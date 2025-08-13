package me.wait.fishyaddons.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.wait.fishyaddons.fishyprotection.BlacklistMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.world.World;

public class ScoreboardUtils {
    private static String gameMode = null;

    private ScoreboardUtils() {}
    
    private static List<String> getSidebarLines() {
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.theWorld;
        if (world == null) {
            gameMode = null;
            return Collections.emptyList();
        }
    
        Scoreboard scoreboard = world.getScoreboard();
        ScoreObjective sidebar = scoreboard.getObjectiveInDisplaySlot(1);
        gameMode = sidebar == null ? null : BlacklistMatcher.stripColor(sidebar.getName());
        
        if (sidebar == null) return Collections.emptyList();
        List<Score> scores = new ArrayList<Score>(scoreboard.getSortedScores(sidebar));
        List<String> lines = new ArrayList<String>();
    
        for (Score score : scores) {
            String playerName = score.getPlayerName();
            ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);
            String line = ScorePlayerTeam.formatPlayerName(team, playerName);
    
            if (line != null && !line.trim().isEmpty()) {
                lines.add(line);
            }
        }
    
        return lines;
    }

    public static String getLine(int index) {
        List<String> lines = getSidebarLines();
        if (index < 0 || index >= lines.size()) return null;
        return BlacklistMatcher.stripColor(lines.get(index));
    }

    public static String getGamemode() {
        getSidebarLines();
        return gameMode;
    }

    // For debugging purposes.
    public static void logSidebar() {
        List<String> sidebar = getSidebarLines();
        for (String line : sidebar) {
            System.out.println("§7[Sidebar] §r" + line);
        }    
    }
}
