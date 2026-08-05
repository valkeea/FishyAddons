package me.valkeea.fishyaddons.api.skyblock;

import me.valkeea.fishyaddons.event.impl.EnvironmentChangeEvent;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.listener.WorldEvent;
import me.valkeea.fishyaddons.util.text.ScoreboardUtils;
import net.minecraft.client.Minecraft;

@SuppressWarnings("squid:S6548")
public class GameMode {

    private boolean isInSkyblock = false;
    private boolean isOnHypixel = false;
    private boolean bypass = false;

    private static final GameMode INSTANCE = new GameMode();
    public static GameMode getInstance() { return INSTANCE; }
    private GameMode() {}

    public boolean checkHypixel(Minecraft mc) {

        var server = mc.getCurrentServer();

        if (server != null) {
            String ip = server.ip.toLowerCase();
            setOnHypixel(ip.contains("hypixel.net"));
            return isOnHypixel;

        } else {
            setOnHypixel(false);
            return false;
        }
    }

    private void setOnHypixel(boolean value) {
        isOnHypixel = value;
    }    

    private boolean checkSkyblock() {
        String title = ScoreboardUtils.getSidebarObjectiveName();
        boolean newStatus = title != null && title.toLowerCase().contains("skyblock");

        sbEvent(newStatus);
        return newStatus;
    }

    /**
     * Perform or re-schedule gamemode check.
     */
    public void updateSkyblockStatus() {
        if (!isOnHypixel && !checkHypixel(Minecraft.getInstance())) {
            isInSkyblock = false;
            return;
        }

        if (bypass && isInSkyblock) {
            bypass = false;
            return;
        }

        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.level.getScoreboard().getObjectives().isEmpty()) {
            isInSkyblock = false;
            WorldEvent.getInstance().reCheck(100);
            return;
        }

        isInSkyblock = checkSkyblock();
    }

    public static void sbEvent(boolean isInSkyblock) {
        if (INSTANCE.isInSkyblock == isInSkyblock) return;
        var event = new EnvironmentChangeEvent(isInSkyblock);
        FaEvents.ENVIRONMENT_CHANGE.firePhased(event, listener -> listener.onEnvironmentChange(event));
    }

    public static void leftSkyblock() {
        sbEvent(false);        
        INSTANCE.isInSkyblock = false;
        INSTANCE.bypass = false;
    }

    /**
     * Bypass the manual check when gamemode has already been confirmed.
     */
    public static void confirm() {
        sbEvent(true);        
        INSTANCE.bypass = true;
        INSTANCE.isInSkyblock = true;
    }    

    /**
     * Check if the player is currently in Hypixel Skyblock.
     */
    public static boolean skyblock() {
        return INSTANCE.isInSkyblock && INSTANCE.isOnHypixel;
    }

    /**
     * Check if the player is currently on Hypixel.
     */
    public static boolean onHypixel() {
        return INSTANCE.isOnHypixel;
    }
}
