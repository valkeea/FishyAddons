package me.valkeea.fishyaddons.feature.skyblock;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.util.TabScanner;
import me.valkeea.fishyaddons.util.text.Color;
import me.valkeea.fishyaddons.util.text.Enhancer;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

@VCModule
public class PetInfo {
    private PetInfo() {}

    private static boolean isOn = false;
    private static boolean tablistReady = false;
    private static int color = 0;

    private static Component l1Scanned = null;
    private static Component directOverride = null;

    private static final Pattern AUTOPET_PATTERN = Pattern.compile(
        "§cAutopet §eequipped your (§.\\[Lvl \\d+\\] (?:§.\\[§.\\d+§.⚔§.\\] )?(?:§.)+.+?)§e! §a§lVIEW RULE");    


    public static boolean handleChat(String s) {
        if (!isOn || !GameMode.skyblock()) return false;

        var directMatcher = AUTOPET_PATTERN.matcher(s);
        if (directMatcher.find()) {
            String petInfoPart = directMatcher.group(1);
            Component petInfo = Enhancer.parseFormattedTextSimple(petInfoPart);
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
            Component msg = Component.literal("Despawned").setStyle(Style.EMPTY.withColor(0xFF808080));
            setOverride(msg);
            clearInfo();
            return true;
        }
        return false;
    }

    @VCListener(
        value = {BooleanKey.HUD_PET_ENABLED, BooleanKey.PET_INCLUDEXP},
        ints = IntKey.HUD_PETXP_COLOR
    ) 
    public static void init() {
        isOn = Config.get(BooleanKey.HUD_PET_ENABLED);
        color = Config.get(IntKey.HUD_PETXP_COLOR);
        ActivePet.forceUpdate();
    }    

    public static void setOverride(Component petInfo) { 
        directOverride = petInfo; 
    }
    
    public static void clearOverride() { 
        directOverride = null; 
    }

    public static Component getPet() {
        if (directOverride != null) return directOverride;
        return l1Scanned != null ? l1Scanned : Component.literal("");
    }

    public static void setNewPet(Component flattened) {
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
        if (l1Scanned == null && isOn) TabScanner.delayedScan();
    }

    public static void setTablistReady(boolean ready) { tablistReady = ready; }
    public static boolean isTablistReady() { return tablistReady; }
    public static boolean isOn() { return isOn; }
    public static boolean shouldIncludeXp() { return Config.get(BooleanKey.PET_INCLUDEXP); }

    public static class ActivePet {
        private static Component l1;
        private static Component l2;
        private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private static ScheduledFuture<?> pendingCombine = null;
        private static final long DEBOUNCE_MS = 50;

        private ActivePet() {}

        public static synchronized void setl1(Component pet) {
            ActivePet.l1 = pet;
            scheduleCombine();
        }        

        public static synchronized void setl2(Component xp) {
            ActivePet.l2 = xp;
            scheduleCombine();
        }

        public static synchronized void forceUpdate() {
            if (l2 != null) {
                scheduleCombine();
            }
        }

        public static void shutdown() {
            scheduler.shutdown();
        }        

        private static synchronized void scheduleCombine() {
            if (pendingCombine != null && !pendingCombine.isDone()) {
                pendingCombine.cancel(false);
            }
            pendingCombine = scheduler.schedule(ActivePet::combine, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        }

        private static synchronized void combine() {

            var merged = Component.empty();
            merged.append(l1);

            if (PetInfo.shouldIncludeXp()) {
                merged.append(Component.literal(" "));

                if (color != 0) merged.append(formatl2(l2, color));
                else merged.append(l2);
            }
            
            clearOverride();
            setNewPet(merged);
        }

        private static Component formatl2(Component line, int color) {

            var raw = line.getString().trim();
            if (raw.equalsIgnoreCase("max level")) {
                return Component.empty().append(Component.literal("MAX").setStyle(Style.EMPTY
                    .withBold(true))
                    .withColor(color));
            }

            var tail = false;
            var result = Component.empty();

            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);

                if (c == 'X' || c == 'P') result.append(paintChar(c, color));
                else if (c == '/' || c == '+' || c == '(' || tail) {
                    result.append(paintChar(c, Color.mulRGB(color, 0.6f)));
                    if (c == '(') tail = true;
                }
                else result.append(paintChar(c, Color.mulRGB(color, 1.2f)));
            }

            return result;
        }

        private static MutableComponent paintChar(char c, int color) {
            return Component.literal(String.valueOf(c)).setStyle(Style.EMPTY.withColor(color));
        }        
    }  
}
