package me.valkeea.fishyaddons.util;

import net.minecraft.client.MinecraftClient;


public class SkyblockCheck {
    private boolean cachedIsInSkyblock = false;
    private boolean cachedIsInHypixel = false;
    private static final SkyblockCheck INSTANCE = new SkyblockCheck();
    private SkyblockCheck() {}

    public static SkyblockCheck getInstance() {
        return INSTANCE;
    }

    public void setInSkyblock(boolean value) {
        this.cachedIsInSkyblock = value;
    }

    public void setInHypixel(boolean value) {
        this.cachedIsInHypixel = value;
    }

    public boolean isInSkyblock() {
        return cachedIsInSkyblock;
    }

    public boolean isInHypixel() {
        return cachedIsInHypixel;
    }

    public boolean rules() {
        return cachedIsInSkyblock && cachedIsInHypixel;
    }

    public void updateHypixelCache() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCurrentServerEntry() != null) {
            String ip = client.getCurrentServerEntry().address.toLowerCase();
            setInHypixel(ip.contains("hypixel.net"));
        } else {
            setInHypixel(false);
        }
    }

    public void updateSkyblockCache() {
        if (!cachedIsInHypixel) {
            cachedIsInSkyblock = false;
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.world.getScoreboard() == null) {
            cachedIsInSkyblock = false;
            return;
        }
        String objective = ScoreboardUtils.getSidebarObjectiveName();
        cachedIsInSkyblock = objective != null && objective.toLowerCase().contains("skyblock");
    }

    public boolean isOnSkyblock() {
        String title = ScoreboardUtils.getSidebarObjectiveName();
        return title.toLowerCase().contains("skyblock");
    }
}