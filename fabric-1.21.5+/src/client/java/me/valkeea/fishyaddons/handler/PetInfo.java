package me.valkeea.fishyaddons.handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.util.SkyblockCheck;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PetInfo {
    private PetInfo() {}
    private static boolean isOn = false;
    private static boolean dynamicMode = false;
    private static boolean tablistReady = false;
    private static long lastScanTime = 0;
    private static boolean needsScan = false;

    public static void refresh() {
        isOn = me.valkeea.fishyaddons.config.FishyConfig.getState(Key.HUD_PET_ENABLED, false);
        dynamicMode = me.valkeea.fishyaddons.config.FishyConfig.getState(Key.HUD_PET_DYNAMIC, false);
    }

    public static void onWorldLoad() {
        setTablistReady(false);
        needsScan = true;
    }

    public static void onTablistReady() {
        setTablistReady(true);
    }

    public static boolean shouldScan() {
        if (!isOn || !SkyblockCheck.getInstance().rules() || !tablistReady) {
            return false;
        }

        long now = System.currentTimeMillis();

        // During summon scan, allow faster detection
        if (TabScanner.isPending()) {
            if (now - lastScanTime >= 200) {
                lastScanTime = now;
                return true;
            }
            return false;
        }
        
        // Dynamic enabled: scan at rate limit + any needed scans
        if (dynamicMode) {
            if (now - lastScanTime >= 1000) {
                lastScanTime = now;
                return true;
            }
            return false;
        }
        
        // Dynamic disabled: only scan when needed
        if (needsScan && now - lastScanTime >= 1000) {
            lastScanTime = now;
            needsScan = false;
            return true;
        }
        
        return false;
    }

    public static void handleChat(String message) {
        if (!isOn || !SkyblockCheck.getInstance().rules()) return;
        
        // Autopet: set direct override
        Pattern directPattern = Pattern.compile("§cAutopet §eequipped your (?:§.)+\\[Lvl \\d+\\] (.+?)(?=§a§lVIEW RULE|$)");
        Matcher directMatcher = directPattern.matcher(message);
        if (directMatcher.find()) {
            String fullMatch = directMatcher.group(0);
            String petInfoPart = fullMatch.substring(fullMatch.indexOf("[Lvl"));
            petInfoPart = petInfoPart.replaceAll("[!¡]+(?:§.)*$", "").replaceAll("§.$", "");
            
            String stripped = Formatting.strip(petInfoPart);
            Text petInfo = Text.literal(petInfoPart);
            TabScanner.setOverride(petInfo);
            Text petOutline = Text.literal(stripped).styled(style -> style.withColor(Formatting.BLACK));
            TabScanner.setOutline(petOutline);
            return;
        }

        // Summon: clear current and schedule scan for new pet confirmation
        Pattern summonPattern = Pattern.compile("You summoned your (.+) ?[!¡]?");
        Matcher summonMatcher = summonPattern.matcher(message);
        if (summonMatcher.find() || message.matches("\\[Lvl \\d+\\] .+")) {
            TabScanner.clearOverride();
            TabScanner.startSummonScan();
            needsScan = true;
        }

        if (message.contains("You despawned your")) {
            Text msg = Text.literal("Despawned")
                .setStyle(Style.EMPTY.withColor(0xFF808080));
            TabScanner.setOverride(msg);
        }
    }

    public static void setTablistReady(boolean ready) { tablistReady = ready; }
    public static boolean isTablistReady() { return tablistReady; }
    public static boolean isOn() { return isOn; }
    public static boolean isDynamic() { return dynamicMode; }
    public static boolean shouldIncludeXp() { 
        return me.valkeea.fishyaddons.config.FishyConfig.getState(Key.HUD_PETXP, false);
    }
}