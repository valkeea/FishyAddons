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
    private boolean verified = false;

    private static final GameMode INSTANCE = new GameMode();
    public static GameMode getInstance() { return INSTANCE; }
    private GameMode() {}

    public static boolean checkHypixel(String ip) {
        if (INSTANCE.verified) return onHypixel();

        boolean newStatus = ip.toLowerCase().contains("hypixel.net");
        if (!newStatus) return false;

        INSTANCE.isOnHypixel = newStatus;
        INSTANCE.verified = true;

        return newStatus;
    }

    private static boolean checkHypixel() {
        if (INSTANCE.verified) return onHypixel();

        var mc = Minecraft.getInstance();
        var server = mc.getCurrentServer();
        if (server == null) return false;
        
        return checkHypixel(server.ip);
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
        if (!isOnHypixel && !checkHypixel()) {
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

    public static void leftServer() {
        INSTANCE.isOnHypixel = false;
        INSTANCE.verified = false;            
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
