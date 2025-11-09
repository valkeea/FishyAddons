package me.valkeea.fishyaddons.feature.skyblock;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.util.TabScanner;
import me.valkeea.fishyaddons.util.text.Enhancer;
import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class PetInfo {
    private PetInfo() {}
    private static boolean isOn = false;
    private static boolean tablistReady = false;

    private static Text l1Scanned = null;
    private static Text directOverride = null;

    private static final Pattern AUTOPET_PATTERN = Pattern.compile(
        "§cAutopet §eequipped your (§.\\[Lvl \\d+\\] (?:§.\\[§.\\d+§.⚔§.\\] )?(?:§.)+.+?)§e! §a§lVIEW RULE");    


    public static boolean handleChat(String s) {
        if (!isOn || !GameMode.skyblock()) return false;

        var directMatcher = AUTOPET_PATTERN.matcher(s);
        if (directMatcher.find()) {
            String petInfoPart = directMatcher.group(1);
            Text petInfo = Enhancer.parseFormattedTextSimple(petInfoPart);
            setOverride(petInfo);
            return true;
        }

        var summonPattern = Pattern.compile("You summoned your (.+) ?[!¡]?");
        var summonMatcher = summonPattern.matcher(s);
        if (summonMatcher.find()) {
            if (l1Scanned == null) TabScanner.delayedScan();
            return true;
        }

        if (s.contains("You despawned your")) {
            Text msg = Text.literal("Despawned").setStyle(Style.EMPTY.withColor(0xFF808080));
            setOverride(msg);
            clearInfo();
            return true;
        }
        return false;
    }

    public static void refresh() {
        isOn = me.valkeea.fishyaddons.config.FishyConfig.getState(Key.HUD_PET_ENABLED, false);
    }    

    public static void setOverride(Text petInfo) { 
        directOverride = petInfo; 
    }
    
    public static void clearOverride() { 
        directOverride = null; 
    }

    public static Text getPet() {
        if (directOverride != null) {
            return directOverride;
        }
        return l1Scanned != null ? l1Scanned : Text.literal("");
    }

    public static void setNewPet(Text flattened) {
        l1Scanned = flattened;
    }

    public static void clearInfo() {
        l1Scanned = null;
    }

    public static void onWorldLoad() {
        setTablistReady(false);
    }

    public static void onTablistReady() {
        setTablistReady(true);

        if (l1Scanned == null) {
            TabScanner.delayedScan();
        }
    }    

    public static void setTablistReady(boolean ready) { tablistReady = ready; }
    public static boolean isTablistReady() { return tablistReady; }
    public static boolean isOn() { return isOn; }
    public static boolean shouldIncludeXp() { 
        return me.valkeea.fishyaddons.config.FishyConfig.getState(Key.HUD_PETXP, false);
    }

    public static class ActivePet {
        private static Text l1;
        private static Text l2;
        private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private static ScheduledFuture<?> pendingCombine = null;
        private static final long DEBOUNCE_MS = 50;

        private ActivePet() {}

        public static synchronized void setl1(Text pet) {
            ActivePet.l1 = pet;
            scheduleCombine();
        }        

        public static synchronized void setl2(Text xp) {
            ActivePet.l2 = xp;
            scheduleCombine();
        }

        private static synchronized void scheduleCombine() {
            if (pendingCombine != null && !pendingCombine.isDone()) {
                pendingCombine.cancel(false);
            }
            pendingCombine = scheduler.schedule(ActivePet::combine, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }

        private static synchronized void combine() {
            MutableText flattened = Text.empty();
            TextUtils.combineToFlat(l1, flattened);

            if (PetInfo.shouldIncludeXp()) {
                flattened.append(Text.literal(" "));
                TextUtils.combineToFlat(l2, flattened);
            }

            clearOverride();
            setNewPet(flattened);
        }
    }  
}
