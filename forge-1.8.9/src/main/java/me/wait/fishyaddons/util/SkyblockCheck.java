package me.wait.fishyaddons.util;

import java.util.ArrayList;
import java.util.List;

import me.wait.fishyaddons.config.ConfigHandler;
import me.wait.fishyaddons.handlers.FishyLavaHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
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

    private static Minecraft getMc() {
        return Minecraft.getMinecraft();
    }

    public void updateSkyblockCache() {
        try {
            if (!cachedIsInHypixel) {
                cachedIsInSkyblock = false;
                return;
            }

            World world = getMc().theWorld;
            if (world == null || world.getScoreboard() == null) {
                cachedIsInSkyblock = false;
                return;
            }

            String objective = ScoreboardUtils.getGamemode();
            if (objective != null) {
                cachedIsInSkyblock = objective.contains("SBScoreboard");
            } else {
                cachedIsInSkyblock = false;
            }

            System.out.println("Skyblock detection: " + cachedIsInSkyblock);
        } catch (Exception e) {
            cachedIsInSkyblock = false;
            System.err.println("Error in Skyblock detection: " + e.getMessage());
        }
    }

    public boolean rules() {
        return cachedIsInSkyblock && cachedIsInHypixel;
    }
}
