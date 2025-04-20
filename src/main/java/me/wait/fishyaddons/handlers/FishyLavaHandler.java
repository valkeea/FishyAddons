package me.wait.fishyaddons.handlers;

import me.wait.fishyaddons.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FishyLavaHandler {

    public static final FishyLavaHandler INSTANCE = new FishyLavaHandler();
    private static boolean isRegistered = false;
    private boolean cachedIsInHypixelSkyblock = false;
    private String lastServerIP = null;
    private int lastDimension = -1;
    private boolean cachedIsFishyLavaEnabled = false;

    private FishyLavaHandler() {
        // Private constructor for singleton
    }

    private static Minecraft getMc() {
        return Minecraft.getMinecraft();
    }
    

    // Dynamically register or unregister the handler
    public static void updateRegistration() {
        if (ConfigHandler.isFishyLavaEnabled()) {
            if (!isRegistered) {
                MinecraftForge.EVENT_BUS.register(INSTANCE);
                isRegistered = true;
            }
        } else {
            if (isRegistered) {
                MinecraftForge.EVENT_BUS.unregister(INSTANCE);
                isRegistered = false;
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (getMc().thePlayer == null || getMc().theWorld == null) return;

        cachedIsFishyLavaEnabled = ConfigHandler.isFishyLavaEnabled();

        if (getMc().theWorld.getScoreboard() == null) return;

        String currentServerIP = getMc().getCurrentServerData() != null ? getMc().getCurrentServerData().serverIP : null;
        if (currentServerIP != null && !currentServerIP.equals(lastServerIP)) {
            lastServerIP = currentServerIP;
            updateSkyblockCache();
        }

        // Check if the player has changed dimensions
        int currentDimension = getMc().thePlayer.dimension;
        if (currentDimension != lastDimension) {
            lastDimension = currentDimension;
            updateSkyblockCache();
        }
    }

    @SubscribeEvent
    public void onClientChatReceived(ClientChatReceivedEvent event) {
        String message = event.message.getUnformattedText();
        if (message.contains("You are playing on profile:")) {
            cachedIsInHypixelSkyblock = true;
        }
    }

    private void updateSkyblockCache() {
        try {
            if (getMc().getCurrentServerData() == null || !getMc().getCurrentServerData().serverIP.toLowerCase().contains("hypixel")) {
                cachedIsInHypixelSkyblock = false;
                System.out.println("Not connected to Hypixel.");
                return;
            }

            World world = getMc().theWorld;
            if (world == null || world.getScoreboard() == null) {
                cachedIsInHypixelSkyblock = false;
                return;
            }

            ScoreObjective objective = world.getScoreboard().getObjectiveInDisplaySlot(1);
            if (objective == null || objective.getDisplayName() == null) {
                cachedIsInHypixelSkyblock = false;
                return;
            }

            cachedIsInHypixelSkyblock = objective.getDisplayName().toUpperCase().contains("SKYBLOCK");
        } catch (Exception e) {
            cachedIsInHypixelSkyblock = false;
            System.err.println("Error in Skyblock detection: " + e.getMessage());
        }
    }

    private boolean isInHypixelSkyblock() {
        return cachedIsInHypixelSkyblock;
    }

    @SubscribeEvent
    public void onFogRender(EntityViewRenderEvent.FogDensity event) {
        if (!cachedIsInHypixelSkyblock || !cachedIsFishyLavaEnabled) {
            return;
        }

        if (isPlayerInLavaAtEyeLevel()) {
            event.density = 0.0F;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onFogColor(EntityViewRenderEvent.FogColors event) {
        if (!cachedIsInHypixelSkyblock || !cachedIsFishyLavaEnabled) {
            return;
        }

        if (isPlayerInLavaAtEyeLevel()) {
            event.red = 1.0F;
            event.green = 1.0F;
            event.blue = 1.0F;
        }
    }

    private boolean isPlayerInLavaAtEyeLevel() {
        if (getMc().thePlayer == null || getMc().theWorld == null) return false;

        double eyeHeight = getMc().thePlayer.posY + getMc().thePlayer.getEyeHeight();

        // Check if the block at the player's eye level is lava
        BlockPos eyeLevelPos = new BlockPos(getMc().thePlayer.posX, eyeHeight, getMc().thePlayer.posZ);
        return getMc().theWorld.getBlockState(eyeLevelPos).getBlock().getMaterial() == Material.lava;
    }
}