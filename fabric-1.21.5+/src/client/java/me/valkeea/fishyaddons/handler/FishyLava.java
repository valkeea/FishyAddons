package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.util.SkyblockCheck;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;   
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Blocks;

public class FishyLava {
    public static boolean shouldRemoveLavaFog(Camera camera) {
        if (!FishyConfig.getState("fishyLava", true)) return false;
        if (MinecraftClient.getInstance().player == null) return false;
        if (!SkyblockCheck.getInstance().rules()) return false;

        Vec3d pos = camera.getPos();
        BlockPos bp = BlockPos.ofFloored(pos);
        return camera.getFocusedEntity().getWorld().getBlockState(bp).isOf(Blocks.LAVA);
    }
}
