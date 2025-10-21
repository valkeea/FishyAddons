package me.valkeea.fishyaddons.handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.util.SkyblockCheck;
import me.valkeea.fishyaddons.util.text.Enhancer;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

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

        if (TabScanner.isPending()) {
            if (now - lastScanTime >= 200) {
                lastScanTime = now;
                return true;
            }
            return false;
        }
        
        if (dynamicMode) {
            if (now - lastScanTime >= 1000) {
                lastScanTime = now;
                return true;
            }
            return false;
        }
        
        if (needsScan && now - lastScanTime >= 1000) {
            lastScanTime = now;
            needsScan = false;
            return true;
        }
        
        return false;
    }

    public static boolean handleChat(String message) {
        if (!isOn || !SkyblockCheck.getInstance().rules()) return false;

        Pattern directPattern = Pattern.compile("§cAutopet §eequipped your (§.\\[Lvl \\d+\\] (?:§.\\[§.\\d+§.⚔§.\\] )?(?:§.)+.+?)§e! §a§lVIEW RULE");
        Matcher directMatcher = directPattern.matcher(message);
        if (directMatcher.find()) {
            String petInfoPart = directMatcher.group(1);
            Text petInfo = Enhancer.parseFormattedTextSimple(petInfoPart);
            TabScanner.setOverride(petInfo);
            return true;
        }

        Pattern summonPattern = Pattern.compile("You summoned your (.+) ?[!¡]?");
        Matcher summonMatcher = summonPattern.matcher(message);
        if (summonMatcher.find() || message.matches("\\[Lvl \\d+\\] .+")) {
            TabScanner.clearOverride();
            TabScanner.startSummonScan();
            needsScan = true;
            return true;
        }

        if (message.contains("You despawned your")) {
            Text msg = Text.literal("Despawned")
                .setStyle(Style.EMPTY.withColor(0xFF808080));
            TabScanner.setOverride(msg);
            return true;
        }
        return false;
    }

    public static void setTablistReady(boolean ready) { tablistReady = ready; }
    public static boolean isTablistReady() { return tablistReady; }
    public static boolean isOn() { return isOn; }
    public static boolean isDynamic() { return dynamicMode; }
    public static boolean shouldIncludeXp() { 
        return me.valkeea.fishyaddons.config.FishyConfig.getState(Key.HUD_PETXP, false);
    }
}