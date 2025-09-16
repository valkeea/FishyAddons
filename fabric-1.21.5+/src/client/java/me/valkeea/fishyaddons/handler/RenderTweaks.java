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
    private static boolean fireFov = false;

    public static void refresh() {
        lavaOn = FishyConfig.getState(Key.FISHY_LAVA, false);
        waterOn = FishyConfig.getState(Key.FISHY_WATER, false);
        fireFov = FishyConfig.getState(Key.FISHY_FIRE_OVERLAY, false);
    }

    public static CameraSubmersionType modifyFogSubmersionType(CameraSubmersionType originalType) {
        if (!SkyblockCheck.getInstance().rules()) return originalType;
        if (TransLava.isEnabled() && originalType == CameraSubmersionType.LAVA) {
            return CameraSubmersionType.WATER;
        }

        return originalType;
    }    

    public static int shouldRemoveLavaFog(Camera camera) {
        if (!lavaOn && !TransLava.isEnabled()) { return 0; }
        if (MinecraftClient.getInstance().player == null) return 0;
        if (!SkyblockCheck.getInstance().rules()) return 0;
        if (camera.getSubmersionType() != CameraSubmersionType.LAVA) {
            return 0;
        }

        Vec3d pos = camera.getPos();
        var bp = BlockPos.ofFloored(pos);
        int ifColor = lavaOn ? 1 : TransLava.getColor();
        
        return camera.getFocusedEntity().getWorld().getBlockState(bp).isOf(Blocks.LAVA) ? ifColor : 0;
    }

    public static boolean shouldRemoveWaterFog(Camera camera) {
        if (!waterOn) return false;
        if (MinecraftClient.getInstance().player == null) return false;
        if (!SkyblockCheck.getInstance().rules()) return false;
        if (camera.getSubmersionType() != CameraSubmersionType.WATER) return false;
        
        Vec3d pos = camera.getPos();
        var bp = BlockPos.ofFloored(pos);

        return camera.getFocusedEntity().getWorld().getBlockState(bp).isOf(Blocks.WATER);
    }

    public static int tryColorFire() {
        if (MinecraftClient.getInstance().player == null) return 0;
        return SkyblockCheck.getInstance().rules() && fireFov
            ? TransLava.getColor() 
            : 0;
    }

    private RenderTweaks() {
        throw new UnsupportedOperationException("Utility class");
    }
}
