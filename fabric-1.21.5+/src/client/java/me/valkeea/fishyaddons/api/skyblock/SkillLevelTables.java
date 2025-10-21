package me.valkeea.fishyaddons.api.skyblock;

import java.util.HashMap;
import java.util.Map;

public class SkillLevelTables {
    
    private static final long[] STANDARD_SKILL_XP = {
        0L, 50L, 175L, 375L, 675L, 1175L, 1925L, 2925L, 4425L, 6425L, 9925L,
        14925L, 22425L, 32425L, 47425L, 67425L, 97425L, 147425L, 222425L, 322425L, 522425L,
        822425L, 1222425L, 1722425L, 2322425L, 3022425L, 3822425L, 4722425L, 5722425L, 6822425L, 8022425L,
        9322425L, 10722425L, 12222425L, 13822425L, 15522425L, 17322425L, 19222425L, 21222425L, 23322425L, 25522425L,
        27822425L, 30222425L, 32722425L, 35322425L, 38072425L, 40972425L, 44072425L, 47472425L, 51172425L, 55172425L,
        59472425L, 64072425L, 68972425L, 74172425L, 79672425L, 85472425L, 91572425L, 97972425L, 104672425L, 111672425L
    };
    
    private static final long[] RUNECRAFTING_XP = {
        0L, 50L, 100L, 125L, 160L, 200L, 250L, 315L, 400L, 500L, 625L,
        785L, 1000L, 1250L, 1600L, 2000L, 2465L, 3125L, 4000L, 5000L, 6200L,
        7800L, 9800L, 12200L, 15300L, 19050L
    };
    
    private static final long[] CATACOMBS_XP = {
        0L, 50L, 75L, 110L, 160L, 230L, 330L, 470L, 670L, 950L, 1340L,
        1890L, 2665L, 3760L, 5260L, 7380L, 10300L, 14400L, 20000L, 27600L, 38000L,
        52500L, 71500L, 97000L, 132000L, 180000L, 243000L, 328000L, 445000L, 600000L, 800000L,
        1065000L, 1410000L, 1900000L, 2500000L, 3300000L, 4300000L, 5600000L, 7200000L, 9200000L, 12000000L,
        15000000L, 19000000L, 24000000L, 30000000L, 38000000L, 48000000L, 60000000L, 75000000L, 93000000L, 116250000L
    };
    
    private static final Map<String, long[]> SKILL_TABLES = new HashMap<>();
    private static final Map<String, Integer> SKILL_MAX_LEVELS = new HashMap<>();
    
    static {

        // Standard skills
        String[] standardSkills = {
            "Farming", "Mining", "Combat", "Foraging", "Fishing", "Enchanting", 
            "Alchemy", "Carpentry", "Taming", "Hunting"
        };
        
        for (String skill : standardSkills) {
            SKILL_TABLES.put(skill, STANDARD_SKILL_XP);
        }

        // Unique XP tables
        SKILL_TABLES.put("Runecrafting", RUNECRAFTING_XP);
        SKILL_MAX_LEVELS.put("Runecrafting", 25);
        
        SKILL_TABLES.put("Catacombs", CATACOMBS_XP);
        SKILL_MAX_LEVELS.put("Catacombs", 50);        
        
        // Max levels for standard skills
        SKILL_MAX_LEVELS.put("Farming", 60);
        SKILL_MAX_LEVELS.put("Mining", 60);
        SKILL_MAX_LEVELS.put("Combat", 60);
        SKILL_MAX_LEVELS.put("Foraging", 54);
        SKILL_MAX_LEVELS.put("Fishing", 50);
        SKILL_MAX_LEVELS.put("Enchanting", 60);
        SKILL_MAX_LEVELS.put("Alchemy", 50);
        SKILL_MAX_LEVELS.put("Carpentry", 60);
        SKILL_MAX_LEVELS.put("Taming", 50);
        SKILL_MAX_LEVELS.put("Hunting", 25);
    }
    
    /**
     * Get the cumulative XP required to reach a specific level
     */
    public static long getXpForLevel(String skillName, int level) {
        long[] table = SKILL_TABLES.get(skillName);
        if (table == null) {
            table = STANDARD_SKILL_XP;
        }
        
        if (level < 0) return 0;
        if (level >= table.length) return table[table.length - 1];
        
        return table[level];
    }
    
    /**
     * Get the XP required to progress from one level to the next
     */
    public static long getXpForNextLevel(String skillName, int currentLevel) {
        if (currentLevel < 0) return 0;
        
        long currentLevelXp = getXpForLevel(skillName, currentLevel);
        long nextLevelXp = getXpForLevel(skillName, currentLevel + 1);
        
        return nextLevelXp - currentLevelXp;
    }
    
    /**
     * Calculate current XP from level and percentage
     */
    public static long calculateCurrentXp(String skillName, int level, double percentage) {
        long baseLevelXp = getXpForLevel(skillName, level);
        long xpForNextLevel = getXpForNextLevel(skillName, level);
        
        long progressXp = Math.round((percentage / 100.0) * xpForNextLevel);
        return baseLevelXp + progressXp;
    }
    
    /**
     * Get the maximum level for a skill
     */
    public static int getMaxLevel(String skillName) {
        Integer maxLevel = SKILL_MAX_LEVELS.get(skillName);
        if (maxLevel != null) {
            return maxLevel;
        }
        
        long[] table = SKILL_TABLES.get(skillName);
        if (table == null) {
            table = STANDARD_SKILL_XP;
        }
        return table.length - 1;
    }
    
    /**
     * Check if a skill is maxed at the given level
     */
    public static boolean isMaxLevel(String skillName, int level) {
        return level >= getMaxLevel(skillName);
    }

    private SkillLevelTables() {}    
}