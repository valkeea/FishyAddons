package me.valkeea.fishyaddons.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.feature.skyblock.PetInfo;
import me.valkeea.fishyaddons.util.text.FromText;
import me.valkeea.fishyaddons.util.text.TablistUtils;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.text.Text;

public class TabScanner {
    private TabScanner() {}
    private static final Pattern PET_PATTERN = Pattern.compile("\\[Lvl \\d+\\] .+");
    private static final Pattern XP_PATTERN = Pattern.compile("^[\\d,]+(?:\\.\\d+)?[kM]?/[\\d,]+(?:\\.\\d+)?[kM]? XP \\(\\d{1,3}(?:\\.\\d+)?%\\)$");

    private static final short FAIL_LIMIT = 10;

    private static int petFails = 0;
    private static int skillFails = 0;

    private static final Object lock = new Object();

    // --- Pet Info ---

    /**
     * Handles tab update packet to scan for pet and XP lines.
     */
    public static void onUpdate(PlayerListS2CPacket packet) {
        if (PetInfo.isOn() && GameMode.skyblock() && !packet.getEntries().isEmpty() &&
            packet.getActions().contains(PlayerListS2CPacket.Action.UPDATE_DISPLAY_NAME)) {

            var displayName = packet.getEntries().get(0).displayName();
            synchronized (lock) {
                if (displayName != null) {

                    var s = displayName.getString();
                    if (isPetLine(s)) {
                        PetInfo.ActivePet.setl1(displayName);

                    } else if (isXpLine(displayName)) {
                        PetInfo.ActivePet.setl2(displayName);
                    }
                }
            }
        }
    }

    /**
     * Check for pet lines on-demand.
     */    
    @SuppressWarnings("squid:S6906")
    public static void delayedScan() {
        synchronized (lock) {
            petFails++;
        }

        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(5000);
                scanPet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private static void scanPet() {
        if (!GameMode.skyblock()) {
            return;
        }

        synchronized (lock) {
            if (petFails == FAIL_LIMIT) {
                FishyNotis.warn(failMsg("active pet"));
            }
        }

        List<Text> lines = TablistUtils.getLines();

        if (lines.isEmpty()) {
            delayedScan();
            return;
        }

        var pet = findl1(lines);
        var xp = findl2(lines);

        if (pet == null || xp == null) {
            delayedScan();

        } else {
            PetInfo.ActivePet.setl1(pet);
            PetInfo.ActivePet.setl2(xp);

            synchronized (lock) {
                petFails = 0;
            }
        }
    }    

    private static Text findl1(List<Text> lines) {
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            if (isPetLine(line.getString())) {
                return line;
            }
        }
        return null;
    }

    private static boolean isPetLine(String line) {
        if (line == null) return false;
        return PET_PATTERN.matcher(line.trim()).matches();
    }

    private static Text findl2(List<Text> lines) {
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            if (isXpLine(line)) {
                return line;
            }
        }
        return null;
    }

    private static boolean isXpLine(Text line) {
        if (line.getSiblings().isEmpty()) {
            return false;
        }

        var targetString = FromText.firstLiteral(line).getString().trim();

        if (targetString.isEmpty()) {
            return false;
        } else if (targetString.equals("MAX LEVEL")) {
            return true;
        }

        return XP_PATTERN.matcher(targetString).matches();
    }
    
    
    // --- Skill Levels ---

    /**
     * Scans tab entries for skill level lines and updates SkillTracker.
     */
    public static boolean scanForSkills() {
        List<Text> lines = TablistUtils.getLines();
        if (lines.isEmpty()) return false;
        
        Map<String, Integer> extractedSkills = new HashMap<>();
        for (Text line : lines) {
            extractedSkills.putAll(extractSkillLevels(line));
        }

        if (!extractedSkills.isEmpty()) {
            for (Map.Entry<String, Integer> entry : extractedSkills.entrySet()) {
                me.valkeea.fishyaddons.tracker.SkillTracker.getInstance().updateSkillLevel(entry.getKey(), entry.getValue());
            }

            synchronized (lock) {
                skillFails = 0;
            }

            return true;
        }        

        synchronized (lock) {
            skillFails++;
            if (skillFails == FAIL_LIMIT) {
                FishyNotis.warn(failMsg("skill level"));
            }
        }

        return false;
    }

    public static Map<String, Integer> extractSkillLevels(Text line) {
        Map<String, Integer> skillLevels = new HashMap<>();
        String lineStr = line.getString();

        java.util.Set<String> validSkills = java.util.Set.of(
            "Farming", "Mining", "Combat", "Foraging", "Fishing", "Enchanting", 
            "Alchemy", "Carpentry", "Taming", "Hunting", "Runecrafting", "Catacombs"
        );

        var skillPattern = Pattern.compile("\\b([A-Za-z]+) (\\d{1,2})(?: ✯)?[: ]");
        var matcher = skillPattern.matcher(lineStr);

        while (matcher.find()) {
            String skillName = matcher.group(1);
            String levelStr = matcher.group(2);
            if (validSkills.contains(skillName)) {
                try {
                    int level = Integer.parseInt(levelStr);
                    skillLevels.put(skillName, level);
                } catch (NumberFormatException e) {
                    // Ignore invalid number formats
                }
            }
        }
        return skillLevels;
    }

    private static String failMsg(String type) {
        return "Failed to find " + type + " info from tab after " + FAIL_LIMIT +
               " attempts. This may be caused by missing tab widgets.";
    }

    public static void reset() {
        synchronized (lock) {
            skillFails = 0;
            petFails = 0;
        }
    }
}
