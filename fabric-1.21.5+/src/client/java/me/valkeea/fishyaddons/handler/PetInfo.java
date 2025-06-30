package me.valkeea.fishyaddons.handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import me.valkeea.fishyaddons.util.SkyblockCheck;

public class PetInfo {
    private PetInfo() {}
    private static boolean isOn = false;
    private static boolean nextCheck = false;
    private static boolean dynamicCheck = false;
    private static boolean xpCheck = false;
    private static boolean tablistState = false;
    private static long lastCheckTime = 0;
    private static boolean widget = false;
    private static boolean newPet = false;
    private static boolean pendingSummonScan = false;

    public static void refresh() {
        isOn = me.valkeea.fishyaddons.config.FishyConfig.getState("petHud", false);
        dynamicCheck = me.valkeea.fishyaddons.config.FishyConfig.getState("tabTicks", false);
        xpCheck = me.valkeea.fishyaddons.config.FishyConfig.getState("petXpCheck", false);
    }

    public static void update() {
        setTablistReady(true);
        setNextCheck(true);       
    }

    public static void confirm(boolean found) {
        newPet = found;
    }

    // Rate-limited or on chat match depending on config
    public static boolean getNextCheck() {
        long now = System.currentTimeMillis();
        boolean shouldCheck = (dynamicCheck || nextCheck) && tablistState &&
        widget && (now - lastCheckTime >= 1000);

        if (shouldCheck) {
            lastCheckTime = now;
            if (nextCheck && newPet) {
                nextCheck = false;
                newPet = false;
            }
        }
        return shouldCheck;
    }

    public static void handleChat(String message) {
        if (!isOn || !SkyblockCheck.getInstance().rules()) return;
        // Autopet: override until next chat or pet change
        Pattern directPattern = Pattern.compile("\\[Lvl \\d+\\] ((?:[^§]|§.)+?)(?=§a§lVIEW RULE|$)");
        Matcher directMatcher = directPattern.matcher(message);
        if (directMatcher.find()) {

            String msg = directMatcher.group();
            msg = msg.replace("!", "");
            String stripped = Formatting.strip(msg);
            Text petInfo = Text.literal(msg);
            TabScanner.setPet(petInfo);
            Text petOutline = Text.literal(stripped).styled(style -> style.withColor(Formatting.BLACK));
            TabScanner.setOutline(petOutline);

            pendingSummonScan = false;
            setNextCheck(false);
            return;
        }

        // Summon: schedule scan, but only update when pet line changes
        Pattern summonPattern = Pattern.compile("You summoned your (.+) ?[!¡]?");
        Matcher summonMatcher = summonPattern.matcher(message);
        if (summonMatcher.find() || message.matches("\\[Lvl \\d+\\] .+")) {
            TabScanner.clearPet();
            TabScanner.clearOutline();
            pendingSummonScan = true;
            setNextCheck(true);
            TabScanner.clearFailCount();
        }

        if (message.contains("You despawned your")) {
            Text msg = Text.literal("You despawned your pet")
            .setStyle(Style.EMPTY.withColor(0xFF808080));
            TabScanner.setPet(msg);
            pendingSummonScan = false;
            setNextCheck(false);
        }
    }

    public static void setNextCheck(boolean allow) { nextCheck = allow; }
    public static void setTablistReady(boolean ready) { tablistState = ready; }
    public static void setPending(boolean pending) { pendingSummonScan = pending; }
    public static void setWidget(boolean found) { widget = found; }
    public static boolean isPending() { return pendingSummonScan; }    
    public static boolean widget() { return widget; }
    public static boolean isDynamic() { return dynamicCheck; }
    public static boolean isOn() { return isOn; }    
    public static boolean shouldScanForXp() { return xpCheck; }
}