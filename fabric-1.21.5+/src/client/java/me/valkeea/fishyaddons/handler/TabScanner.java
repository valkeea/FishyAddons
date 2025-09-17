package me.valkeea.fishyaddons.handler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.text.TablistUtils;
import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TabScanner {
    private TabScanner() {}
    private static final Pattern PET_PATTERN = Pattern.compile("\\[Lvl \\d+\\] .+");
    private static final int FAIL_LIMIT = 20;

    // Direct overrides
    private static Text directOverride = null;
    private static Text overrideOutline = null;

    // Tab-scanned pet info
    private static PetTabInfo l1Scanned = null;
    
    // Summon handling
    private static boolean pendingSummon = false;
    private static String prevl1 = null;
    private static long summonStartTime = 0;
    private static int failCount = 0;
    private static final int MIN_SUMMON_WAIT_MS = 500;
    private static final int MAX_SUMMON_WAIT_MS = 5000;

    public static void setOverride(Text petInfo) { 
        directOverride = petInfo; 
        clearPet();
    }
    
    public static void setOutline(Text outline) { 
        overrideOutline = outline; 
    }
    
    public static void clearOverride() { 
        directOverride = null; 
        overrideOutline = null;
    }

    public static void startSummonScan() {
        String currentPetText = l1Scanned != null ? extract(l1Scanned.l1) : null;
        prevl1 = currentPetText;
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
        // Priority: direct override > scanned pet
        if (directOverride != null) {
            return directOverride;
        }
        return l1Scanned != null ? l1Scanned.getCombined() : null;
    }

    public static Text getOutline() {
        if (overrideOutline != null) {
            return overrideOutline;
        }
        if (directOverride != null) {
            String stripped = Formatting.strip(directOverride.getString());
            return Text.literal(stripped).styled(style -> style.withColor(Formatting.BLACK));
        }
        return l1Scanned != null ? l1Scanned.getCombinedForOutline() : null;
    }

    public static void onUpdate() {
        if (pendingSummon && (System.currentTimeMillis() - summonStartTime > MAX_SUMMON_WAIT_MS * 2)) {
            pendingSummon = false;
            prevl1 = null;
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
                prevl1 = null;
                summonStartTime = 0;
                failCount = 0;
                sendFailNotification();
                return;
            }
            
            failCount++;
            if (failCount >= FAIL_LIMIT) {
                pendingSummon = false;
                prevl1 = null;
                summonStartTime = 0;
                failCount = 0;
                sendFailNotification();
            }
        } else {

            clearPet();
        }
    }

    private static void handleSummon(PetLineResult l1Result) {
        String currentl1 = l1Result.lineText;
        long timeSinceSummon = System.currentTimeMillis() - summonStartTime;
        
        if (timeSinceSummon > MAX_SUMMON_WAIT_MS) {
            l1Scanned = new PetTabInfo(l1Result.line, null);
            pendingSummon = false;
            prevl1 = null;
            summonStartTime = 0;
            failCount = 0;
            return;
        }
        
        if (timeSinceSummon < MIN_SUMMON_WAIT_MS) {
            return;
        }
        
        String currentPetId = getIdentifier(currentl1);
        String prevPetId = getIdentifier(prevl1);
        boolean hasChanged = prevPetId == null || !currentPetId.equals(prevPetId);

        if (hasChanged) {
            l1Scanned = new PetTabInfo(l1Result.line, null);
            pendingSummon = false;
            prevl1 = null;
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
        
        l1Scanned = new PetTabInfo(l1, l2);
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

    private static String extract(Text l1) {
        if (l1 == null) return null;
        String fullText = l1.getString().trim();
        return Formatting.strip(fullText);
    }

    private static String getIdentifier(String l1) {
        if (l1 == null) return null;

        String stripped = Formatting.strip(l1);
        Matcher matcher = PET_PATTERN.matcher(stripped);
        if (matcher.matches()) {
            return stripped;
        }
        return stripped;
    }

    private static boolean isPetLine(String line) {
        if (line == null) return false;
        Matcher m = PET_PATTERN.matcher(line.trim());
        return m.matches();
    }

    private static boolean isXpLine(Text line) {
        List<Text> siblings = line.getSiblings();
        
        if (siblings.size() == 2) {
            Text maxLevel = siblings.get(1);
            if (maxLevel.getString().trim().equals("MAX LEVEL") &&
                maxLevel.getStyle().getColor() != null &&
                maxLevel.getStyle().getColor().getRgb() == 0x55FFFF &&
                Boolean.TRUE.equals(maxLevel.getStyle().isBold())) {
                return true;
            }
        }
        
        if (siblings.size() >= 5) {
            Text t1 = siblings.get(1);
            Text t2 = siblings.get(2);
            Text t3 = siblings.get(3);
            Text t4 = siblings.get(4);

            String s1 = t1.getString().trim();
            String s2 = t2.getString().trim();
            String s3 = t3.getString().trim();
            String s4 = t4.getString().trim();

            return t1.getStyle().getColor() != null && t1.getStyle().getColor().getRgb() == 0xFFFF55 && s1.matches("[\\d,\\.]+") &&
                   t2.getStyle().getColor() != null && t2.getStyle().getColor().getRgb() == 0xFFAA00 && s2.equals("/") &&
                   t3.getStyle().getColor() != null && t3.getStyle().getColor().getRgb() == 0xFFFF55 && s3.endsWith("XP") &&
                   t4.getStyle().getColor() != null && t4.getStyle().getColor().getRgb() == 0xFFAA00 && s4.matches("\\([\\d\\.]+%\\)");
        }
        return false;
    }

    private static void sendFailNotification() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            FishyNotis.send(
                Text.literal("Failed to find pet info. Please check your tab widgets.")
                    .formatted(net.minecraft.util.Formatting.RED)
            );
        }
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
    }

    public static class PetTabInfo {
        private final Text l1;
        private final Text l2;

        public PetTabInfo(Text l1, Text l2) {
            this.l1 = l1;
            this.l2 = l2;
        }

        public Text getCombined() {
            if (l2 != null) {
                return Text.empty().append(l1).append(Text.literal(" ")).append(l2);
            } else {
                return l1;
            }
        }

        public Text getCombinedForOutline() {
            Text combined = getCombined();
            return TextUtils.recolor(combined);
        }
    }
}