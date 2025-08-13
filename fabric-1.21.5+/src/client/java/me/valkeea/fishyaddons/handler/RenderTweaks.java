package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.util.SkyblockCheck;
import me.valkeea.fishyaddons.util.AreaUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;   
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Blocks;

public class RenderTweaks {
    private RenderTweaks() {}
    public static boolean shouldRemoveLavaFog(Camera camera) {
        if (!FishyConfig.getState(Key.FISHY_LAVA, false)) return false;
        if (MinecraftClient.getInstance().player == null) return false;
        if (!SkyblockCheck.getInstance().rules()) return false;

        Vec3d pos = camera.getPos();
        BlockPos bp = BlockPos.ofFloored(pos);

        return camera.getFocusedEntity().getWorld().getBlockState(bp).isOf(Blocks.LAVA);
    }

    public static boolean shouldRemoveWaterFog(Camera camera) {
        if (!FishyConfig.getState(Key.FISHY_WATER, false)) return false;
        if (MinecraftClient.getInstance().player == null) return false;
        if (!SkyblockCheck.getInstance().rules()) return false;

        Vec3d pos = camera.getPos();
        BlockPos bp = BlockPos.ofFloored(pos);

        return camera.getFocusedEntity().getWorld().getBlockState(bp).isOf(Blocks.WATER);
    }
}
