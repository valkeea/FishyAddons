package me.valkeea.fishyaddons.feature.visual;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.feature.skyblock.TransLava;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;

public class RenderTweaks {
    private static boolean lavaOn = false;
    private static boolean waterOn = false;
    private static boolean fireFov = false;

    public static void refresh() {
        lavaOn = FishyConfig.getState(Key.FISHY_LAVA, false);
        waterOn = FishyConfig.getState(Key.FISHY_WATER, false);
        fireFov = FishyConfig.getState(Key.FISHY_FIRE_OVERLAY, false);
    }

    public static boolean shouldRemoveWaterFog(Camera camera) {
        if (!waterOn) return false;
        if (MinecraftClient.getInstance().player == null) return false;
        if (!GameMode.skyblock()) return false;
        if (camera.getSubmersionType() != CameraSubmersionType.WATER) return false;
        
        var pos = camera.getCameraPos();
        var bp = BlockPos.ofFloored(pos);

        return camera.getFocusedEntity().getEntityWorld().getBlockState(bp).isOf(Blocks.WATER);
    }    

    /**
     * Determines if lava fog should be removed or tinted.
     * @param camera The camera instance.
     * @return 0 if no change, 1 for removal, or the tint color integer.
     */
    public static int shouldRemoveLavaFog(Camera camera) {
        if (!lavaOn && !TransLava.isEnabled()) { return 0; }
        if (MinecraftClient.getInstance().player == null) return 0;
        if (!GameMode.skyblock()) return 0;
        if (camera.getSubmersionType() != CameraSubmersionType.LAVA) {
            return 0;
        }

        var pos = camera.getCameraPos();
        var bp = BlockPos.ofFloored(pos);
        int ifColor = lavaOn ? 1 : TransLava.getColor();

        return camera.getFocusedEntity().getEntityWorld().getBlockState(bp).isOf(Blocks.LAVA) ? ifColor : 0;
    }

    /**
     * Determines the fire overlay color.
     * @return 0 for default, or the tint color integer.
     */
    public static int tryColorFire() {
        if (MinecraftClient.getInstance().player == null) return 0;
        return GameMode.skyblock() && fireFov
            ? TransLava.getColor() 
            : 0;
    }

    private RenderTweaks() {
        throw new UnsupportedOperationException("Utility class");
    }
}
