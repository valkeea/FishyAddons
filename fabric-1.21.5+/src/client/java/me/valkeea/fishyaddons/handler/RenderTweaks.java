package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.util.SkyblockCheck;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class RenderTweaks {
    private static boolean lavaOn = false;
    private static boolean waterOn = false;


    public static void refresh() {
        lavaOn = FishyConfig.getState(Key.FISHY_LAVA, false);
        waterOn = FishyConfig.getState(Key.FISHY_WATER, false);
    }

    public static CameraSubmersionType modifyFogSubmersionType(CameraSubmersionType originalType) {
        if (!SkyblockCheck.getInstance().rules()) return originalType;

        var lavaIsWater = FishyConfig.getState(Key.FISHY_TRANS_LAVA, false);
        if (lavaIsWater && originalType == CameraSubmersionType.LAVA) {
            return CameraSubmersionType.WATER;
        }

        return originalType;
    }

    public static boolean shouldRemoveLavaFog(Camera camera) {
        if (!lavaOn) return false;
        if (MinecraftClient.getInstance().player == null) return false;
        if (!SkyblockCheck.getInstance().rules()) return false;
        if (camera.getSubmersionType() != CameraSubmersionType.LAVA) return false;

        Vec3d pos = camera.getPos();
        BlockPos bp = BlockPos.ofFloored(pos);

        return camera.getFocusedEntity().getWorld().getBlockState(bp).isOf(Blocks.LAVA);
    }

    public static boolean shouldRemoveWaterFog(Camera camera) {
        if (!waterOn) return false;
        if (MinecraftClient.getInstance().player == null) return false;
        if (!SkyblockCheck.getInstance().rules()) return false;
        if (camera.getSubmersionType() != CameraSubmersionType.WATER) return false;

        Vec3d pos = camera.getPos();
        BlockPos bp = BlockPos.ofFloored(pos);

        return camera.getFocusedEntity().getWorld().getBlockState(bp).isOf(Blocks.WATER);
    }

    private RenderTweaks() {
        throw new UnsupportedOperationException("Utility class");
    }
}
