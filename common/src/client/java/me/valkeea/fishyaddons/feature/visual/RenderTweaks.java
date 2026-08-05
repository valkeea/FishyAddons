package me.valkeea.fishyaddons.feature.visual;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.feature.skyblock.TransLava;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
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
    private static int skyColor = 0x00000000;

    @VCListener({BooleanKey.FISHY_LAVA, BooleanKey.FISHY_WATER, BooleanKey.FIRE_OVERLAY})
    public static void refresh() {
        clearLava = Config.get(BooleanKey.FISHY_LAVA);
        clearWater = Config.get(BooleanKey.FISHY_WATER);
        fireFov = Config.get(BooleanKey.FIRE_OVERLAY);
    }

    @VCListener(ints = IntKey.SKY_COLOR)
    public static void refreshSkyColor(int color) {
        skyColor = color;
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

    public static FogModifier getFogModifier(Camera camera) {
        if (!validEnv()) return FogModifier.NONE;
        return camera.getFluidInCamera() == FogType.LAVA ? getLavaFogModifier(camera) : getSkyFogModifier(camera);
    }

    /**
     * Determines if lava fog should be removed or tinted.
     */
    public static FogModifier getLavaFogModifier(Camera camera) {
        if (!clearLava && !TransLava.isEnabled()) return FogModifier.NONE;

        var entity = camera.entity();
        if (entity == null) return FogModifier.NONE;

        var pos = camera.position();
        var bp = BlockPos.containing(pos);
        if (!entity.level().getBlockState(bp).is(Blocks.LAVA)) return FogModifier.NONE;

        return clearLava ? FogModifier.REMOVE : FogModifier.tint(TransLava.getColor());
    }

    private static FogModifier getSkyFogModifier(Camera camera) {
        if (!SkyblockAreas.isCrimson()) return FogModifier.NONE;
        return skyColor != 0x00000000 ? FogModifier.sky(skyColor) : FogModifier.NONE;
    }

    /**
     * Determines the fire overlay color.
     * @return 0 for default, or the tint color integer.
     */
    public static int tryColorFire() {
        if (!validEnv()) return 0;
        return fireFov ? TransLava.getColor() : 0;
    }

    public static int getSkyColor() {
        return skyColor;
    }

    private static boolean validEnv() {
        var mc = Minecraft.getInstance();
        return mc.player != null && GameMode.skyblock();
    }

    public record FogModifier(Kind kind, int color) {

        public enum Kind { NONE, REMOVE, TINT, SKY }

        public static final FogModifier NONE = new FogModifier(Kind.NONE, 0);
        public static final FogModifier REMOVE = new FogModifier(Kind.REMOVE, 0);

        public static FogModifier tint(int color) {
            return new FogModifier(Kind.TINT, color);
        }

        public static FogModifier sky(int color) {
            return new FogModifier(Kind.SKY, color);
        }

        public boolean isNone() {
            return kind == Kind.NONE;
        }

        public boolean isRemove() {
            return kind == Kind.REMOVE;
        }

        public boolean isTint() {
            return kind == Kind.TINT;
        }

        public boolean isSky() {
            return kind == Kind.SKY;
        }
    }

    private RenderTweaks() {}
}
