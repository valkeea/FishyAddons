package me.valkeea.fishyaddons.feature.visual;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.feature.skyblock.TransLava;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FogType;

@VCModule
public class RenderTweaks {
    private static boolean clearLava = false;
    private static boolean clearWater = false;
    private static boolean fireFov = false;

    @VCListener({BooleanKey.FISHY_LAVA, BooleanKey.FISHY_WATER, BooleanKey.FIRE_OVERLAY})
    public static void refresh() {
        clearLava = Config.get(BooleanKey.FISHY_LAVA);
        clearWater = Config.get(BooleanKey.FISHY_WATER);
        fireFov = Config.get(BooleanKey.FIRE_OVERLAY);
    }

    public static boolean shouldRemoveWaterFog(Camera camera) {
        if (!clearWater) return false;
        if (!validEnv() || camera.getFluidInCamera() != FogType.WATER) return false;
        
        var pos = camera.position();
        var bp = BlockPos.containing(pos);
        var entity = camera.entity();
        
        if (entity == null) return false;

        return entity.level().getBlockState(bp).is(Blocks.WATER);
    }    

    /**
     * Determines if lava fog should be removed or tinted.
     * @param camera The camera instance.
     * @return 0 if no change, 1 for removal, or the tint color integer.
     */
    public static int shouldRemoveLavaFog(Camera camera) {
        if (!clearLava && !TransLava.isEnabled()) return 0;
        if (!validEnv() || camera.getFluidInCamera() != FogType.LAVA) return 0;

        var pos = camera.position();
        var bp = BlockPos.containing(pos);
        int ifColor = clearLava ? 1 : TransLava.getColor();
        var entity = camera.entity();

        if (entity == null) return 0;

        return entity.level().getBlockState(bp).is(Blocks.LAVA) ? ifColor : 0;
    }

    /**
     * Determines the fire overlay color.
     * @return 0 for default, or the tint color integer.
     */
    public static int tryColorFire() {
        if (!validEnv()) return 0;
        return fireFov ? TransLava.getColor() : 0;
    }

    private static boolean validEnv() {
        var mc = Minecraft.getInstance();
        return mc.player != null && GameMode.skyblock();
    }

    private RenderTweaks() {}
}
