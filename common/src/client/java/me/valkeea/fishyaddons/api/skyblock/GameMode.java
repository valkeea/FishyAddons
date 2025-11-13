package me.valkeea.fishyaddons.api.skyblock;

import me.valkeea.fishyaddons.event.impl.EnvironmentChangeEvent;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.listener.WorldEvent;
import me.valkeea.fishyaddons.util.text.ScoreboardUtils;
import net.minecraft.client.MinecraftClient;

@SuppressWarnings("squid:S6548")
public class GameMode {

    private boolean isInSkyblock = false;
    private boolean isInHypixel = false;
    private boolean bypass = false;

    private static final GameMode INSTANCE = new GameMode();
    public static GameMode getInstance() { return INSTANCE; }
    private GameMode() {}

    private boolean checkHypixel() {

        var client = MinecraftClient.getInstance();

        if (client.getCurrentServerEntry() != null) {
            String ip = client.getCurrentServerEntry().address.toLowerCase();
            setInHypixel(ip.contains("hypixel.net"));
            return isInHypixel;

        } else {
            setInHypixel(false);
            return false;
        }
    }

    private void setInHypixel(boolean value) {
        isInHypixel = value;
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

        if (!isInHypixel && !checkHypixel()) {
            isInSkyblock = false;
            return;
        }

        if (bypass && isInSkyblock) {
            bypass = false;
            return;
        }

        var client = MinecraftClient.getInstance();
        if (client.world == null || client.world.getScoreboard() == null) {
            isInSkyblock = false;
            WorldEvent.getInstance().reCheck(100);
            return;
        }

        isInSkyblock = checkSkyblock();
    }

    private static void sbEvent(boolean isInSkyblock) {
        if (INSTANCE.isInSkyblock == isInSkyblock) return;
        var event = new EnvironmentChangeEvent(isInSkyblock);
        FaEvents.ENVIRONMENT_CHANGE.firePhased(event, listener -> listener.onEnvironmentChange(event));
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
        return INSTANCE.isInSkyblock && INSTANCE.isInHypixel;
    }
}
