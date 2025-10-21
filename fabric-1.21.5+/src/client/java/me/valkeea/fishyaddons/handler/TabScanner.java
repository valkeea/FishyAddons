package me.valkeea.fishyaddons.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.FromText;
import me.valkeea.fishyaddons.util.text.TablistUtils;
import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TabScanner {
    private TabScanner() {}
    private static final Pattern PET_PATTERN = Pattern.compile("\\[Lvl \\d+\\] .+");

    private static final int FAIL_LIMIT = 20;
    private static final int MIN_SUMMON_WAIT_MS = 500;
    private static final int MAX_SUMMON_WAIT_MS = 5000;

    private static Text directOverride = null;
    private static PetTabInfo l1Scanned = null;

    private static boolean pendingSummon = false;
    private static long summonStartTime = 0;
    private static int failCount = 0;
    private static int skillFailCount = 0;

    public static void setOverride(Text petInfo) { 
        directOverride = petInfo; 
        clearPet();
    }
    
    public static void clearOverride() { 
        directOverride = null; 
    }

    public static void startSummonScan() {
        pendingSummon = true;
        summonStartTime = System.currentTimeMillis();
        failCount = 0;
        clearPet();
    }

    public static boolean isPending() {
        return pendingSummon;
    }

    private static void clearPet() {
        l1Scanned = null;
    }

    public static Text getPet() {
        if (directOverride != null) {
            return directOverride;
        }
        return l1Scanned != null ? l1Scanned.getFlat() : Text.literal("");
    }

    public static void onUpdate() {
        if (pendingSummon && (System.currentTimeMillis() - summonStartTime > MAX_SUMMON_WAIT_MS * 2)) {
            pendingSummon = false;
            summonStartTime = 0;
            failCount = 0;
        }

        if (PetInfo.isDynamic()) {
            clearOverride();
        }

        List<Text> lines = TablistUtils.getLines();

        if (lines.isEmpty()) {
            clearPet();
            return;
        }

        PetLineResult l1Result = findPetLine(lines);
        if (l1Result == null) {
            noneFound();
            return;
        }

        if (pendingSummon) {
            handleSummon(l1Result);
            return;
        }

        updateScannedPet(l1Result, lines);
        failCount = 0;
    }

    private static PetLineResult findPetLine(List<Text> lines) {
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            if (isPetLine(line.getString())) {
                return new PetLineResult(i, line, line.getString().trim());
            }
        }
        return null;
    }
 
    private static void noneFound() {
        if (pendingSummon) {
            long timeSinceSummon = System.currentTimeMillis() - summonStartTime;

            if (timeSinceSummon > MAX_SUMMON_WAIT_MS) {
                pendingSummon = false;
                summonStartTime = 0;
                failCount = 0;
                sendFailNotification();
                return;
            }
            
            failCount++;
            if (failCount >= FAIL_LIMIT) {
                pendingSummon = false;
                summonStartTime = 0;
                failCount = 0;
                sendFailNotification();
            }
        } else {

            clearPet();
        }
    }

    private static void handleSummon(PetLineResult l1Result) {
        long timeSinceSummon = System.currentTimeMillis() - summonStartTime;
        
        if (timeSinceSummon > MAX_SUMMON_WAIT_MS) {
            l1Scanned = new PetTabInfo(l1Result.line, l1Result.getPetId());
            pendingSummon = false;
            summonStartTime = 0;
            failCount = 0;
            return;
        }
        
        if (timeSinceSummon < MIN_SUMMON_WAIT_MS) {
            return;
        }
        
        String currentPetId = l1Result.getPetId();
        String prevPetId = l1Scanned != null ? l1Scanned.getPet() : "";
        boolean hasChanged = prevPetId == null || !currentPetId.equals(prevPetId);

        if (hasChanged) {
            l1Scanned = new PetTabInfo(l1Result.line, l1Result.getPetId());
            pendingSummon = false;
            summonStartTime = 0;
            failCount = 0;

        } else {
            failCount++;
        }
    }

    private static void updateScannedPet(PetLineResult l1Result, List<Text> lines) {
        Text l1 = l1Result.line;
        Text l2 = null;

        if (PetInfo.isDynamic() && PetInfo.shouldIncludeXp()) {
            l2 = findXpLine(lines, l1Result.index);
        }
        
        MutableText flattened = Text.empty();
        TextUtils.combineToFlat(l1, flattened);
        
        if (l2 != null) {
            flattened.append(Text.literal(" "));
            TextUtils.combineToFlat(l2, flattened);
        }

        l1Scanned = new PetTabInfo(flattened, l1Result.getPetId());
    }

    private static Text findXpLine(List<Text> lines, int l1Idx) {
        for (int i = 0; i < lines.size(); i++) {
            if (i == l1Idx) continue;
            if (isXpLine(lines.get(i))) {
                return lines.get(i);
            }
        }
        return null;
    }

    private static boolean isPetLine(String line) {
        if (line == null) return false;
        Matcher m = PET_PATTERN.matcher(line.trim());
        return m.matches();
    }

    private static boolean isXpLine(Text line) {
        if (line.getSiblings().isEmpty()) {
            return false;
        }
        var target = FromText.firstLiteral(line);
        return target != null && (target.getString().trim().contains("%)") || target.getString().trim().equals("MAX LEVEL"));
    }

    private static void sendFailNotification() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            FishyNotis.warn(
                "FishyAddons failed to find pet info after 20 attempts. This may be caused by missing tab info or an incompatible mod.");
        }
    }

    public static boolean scanForSkills() {
        List<Text> lines = TablistUtils.getLines();
        if (lines.isEmpty()) return false;
        
        for (Text line : lines) {
            Map<String, Integer> extractedSkills = extractSkillLevels(line);
            if (!extractedSkills.isEmpty()) {
                
                for (Map.Entry<String, Integer> entry : extractedSkills.entrySet()) {
                    me.valkeea.fishyaddons.tracker.SkillTracker.getInstance().updateSkillLevel(entry.getKey(), entry.getValue());
                }
                return true;
            }
        }

        skillFailCount++;

        if (skillFailCount >= 10) {
            skillFailCount = 0;
            FishyNotis.warn(
                "FishyAddons failed to extract skill levels from tab after 10 attempts. This may be caused by missing tab info or an incompatible mod.");
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

    private static class PetLineResult {
        final int index;
        final Text line;
        final String lineText;

        PetLineResult(int index, Text line, String lineText) {
            this.index = index;
            this.line = line;
            this.lineText = lineText;
        }

        String getPetId() {
            String stripped = Formatting.strip(lineText);
            return stripped.replaceAll("^\\[Lvl \\d+\\] ", "").trim();
        }
    }

    public static class PetTabInfo {
        private final Text flattened;
        private final String pet;

        public PetTabInfo(Text flattened, String pet) {
            this.flattened = flattened;
            this.pet = pet;
        }

        public Text getFlat() {
            return flattened;
        }

        public String getPet() {
            return pet;
        }
    }
}