package me.wait.fishyaddons.handlers;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.util.SkyblockCheck;
import net.minecraft.client.Minecraft;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FishyLavaHandler {

    public static final FishyLavaHandler INSTANCE = new FishyLavaHandler();
    private static boolean isRegistered = false;

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
    public void onFogRender(EntityViewRenderEvent.FogDensity event) {
        if (!SkyblockCheck.getInstance().rules()) {
            return;
        }

        if (isPlayerInLavaAtEyeLevel()) {
            event.density = 0.0F;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onFogColor(EntityViewRenderEvent.FogColors event) {
        if (!SkyblockCheck.getInstance().rules()) {
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