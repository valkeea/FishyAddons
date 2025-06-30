package me.valkeea.fishyaddons.handler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.util.FishyNotis;
import me.valkeea.fishyaddons.util.TablistUtils;
import me.valkeea.fishyaddons.util.TextUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TabScanner {
    private TabScanner() {}
    private static final Pattern PET_PATTERN = Pattern.compile("\\[Lvl \\d+\\] .+");
    private static final int XP_FAIL_LIMIT = 15;
    private static PetTabInfo saved = null;
    private static int xpFailCount = 0;
    private static Text override = null;
    private static Text overrideOutline = null;
    private static int failCount = 0;
    private static final int FAIL_LIMIT = 20;
    private static final java.util.Map<String, Text> maxLevelCache = new java.util.HashMap<>();

    private static String lastline1 = null;
    private static int summonFailCount = 0;
    private static final int SUMMON_FAIL_LIMIT = 15;

    public static void setPet(Text petInfo) { override = petInfo; }
    public static void clearPet() { override = null; }
    public static void setOutline(Text outline) { overrideOutline = outline; }
    public static void clearOutline() { overrideOutline = null; }
    public static void clearFailCount() { failCount = 0; }

    public static Text getPet() {
        if (override != null) {
            return override;
        }
        return saved != null ? saved.getCombined() : null;
    }

    public static Text getOutline() {
        if (overrideOutline != null) {
            return overrideOutline;
        }
        if (override != null) {
            String stripped = Formatting.strip(override.getString());
            Text outline = Text.literal(stripped).styled(style -> style.withColor(Formatting.BLACK));
            return outline;
        }
        return saved != null ? saved.getCombinedForOutline() : null;
    }

    public static void onUpdate() {
        if (PetInfo.isDynamic()) {
            clearPet();
            clearOutline();
        }
        List<Text> lines = TablistUtils.getLines();
        if (lines.isEmpty()) {
            saved = null;
            return;
        }

        String currentline1 = null;
        int line1Idx = -1;

        // Find the pet line index and value
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            if (isPetLine(line.getString())) {
                currentline1 = line.getString().trim();
                line1Idx = i;
                break;
            }
        }

        if (PetInfo.isPending()) {
            handlePending(currentline1);
        }

        if (line1Idx == -1) {
            saved = null;
            return;
        }

        String petKey = currentline1;
        Text line1 = lines.get(line1Idx);

        // Try to find a xp line line if dynamic
        if (PetInfo.isDynamic() && PetInfo.shouldScanForXp()) {
            int contIdx = findXpLineIdx(lines, line1Idx);
            if (contIdx != -1) {
                Text cont = lines.get(contIdx);
                if (isMaxLevelLine(cont)) {
                    maxLevelCache.put(petKey, cont);
                }
                saved = new PetTabInfo(line1Idx, contIdx, line1, cont);
                xpFailCount = 0;
                failCount = 0;
                return;
            }

            Text foundMaxLevel = maxLevelCache.get(petKey);
            if (foundMaxLevel != null) {
                saved = new PetTabInfo(line1Idx, -1, line1, foundMaxLevel);
                xpFailCount = 0;
                failCount = 0;
                return;
            }
            
            xpFailCount++;
            failCount++;
            if (xpFailCount >= XP_FAIL_LIMIT || failCount >= FAIL_LIMIT) {
                sendFailNoti();
                xpFailCount = 0;
                failCount = 0;
            }
        }
    }

    // Find the index of a valid xp line, or -1 if not found
    private static int findXpLineIdx(List<Text> lines, int line1Idx) {
        for (int j = 0; j < lines.size(); j++) {
            if (j == line1Idx) continue;
            if (isXpLine(lines.get(j))) {
                return j;
            }
        }
        return -1;
    }

    // Handle pending summon scan logic
    private static void handlePending(String currentline1) {
        lastline1 = saved != null ? saved.line1.getString().trim() : null;
        // Only proceed if info has changed
        if (currentline1 != null && !currentline1.equals(lastline1)) {
            PetInfo.confirm(true);
            PetInfo.setPending(false);            
            summonFailCount = 0;
        } else {
            summonFailCount++;
            if (summonFailCount >= SUMMON_FAIL_LIMIT) {
                PetInfo.setPending(false);
                PetInfo.setNextCheck(false);
                summonFailCount = 0;
                sendFailNoti();
            }
        }
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

            boolean match =
                t1.getStyle().getColor() != null && t1.getStyle().getColor().getRgb() == 0xFFFF55 && s1.matches("[\\d,\\.]+") &&
                t2.getStyle().getColor() != null && t2.getStyle().getColor().getRgb() == 0xFFAA00 && s2.equals("/") &&
                t3.getStyle().getColor() != null && t3.getStyle().getColor().getRgb() == 0xFFFF55 && s3.endsWith("XP") &&
                t4.getStyle().getColor() != null && t4.getStyle().getColor().getRgb() == 0xFFAA00 && s4.matches("\\([\\d\\.]+%\\)");

            if (match) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMaxLevelLine(Text line) {
        List<Text> siblings = line.getSiblings();
        if (siblings.size() == 2) {
            Text maxLevel = siblings.get(1);
            return maxLevel.getString().trim().equals("MAX LEVEL") &&
                   maxLevel.getStyle().getColor() != null &&
                   maxLevel.getStyle().getColor().getRgb() == 0x55FFFF &&
                   Boolean.TRUE.equals(maxLevel.getStyle().isBold());
        }
        return false;
    }

    private static void sendFailNoti() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            FishyNotis.send(
                Text.literal("Failed to find pet info. Please check your tab widgets.")
                    .formatted(net.minecraft.util.Formatting.RED)
            );
        }
    }

    public static class PetTabInfo {
        public final int line1Idx;
        public final int line2Idx;
        public final Text line1;
        public final Text line2;

        public PetTabInfo(int line1Idx, int line2Idx, Text line1, Text line2) {
            this.line1Idx = line1Idx;
            this.line2Idx = line2Idx;
            this.line1 = line1;
            this.line2 = line2;
        }

        public Text getCombined() {
            if (line2 != null) {
                return Text.empty().append(line1).append(Text.literal(" ")).append(line2);
            } else {
                return line1;
            }
        }

        public Text getCombinedForOutline() {
            Text combined = getCombined();
            return TextUtils.recolor(combined);
        }      
    }
}