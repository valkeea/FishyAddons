package me.valkeea.fishyaddons.feature.skyblock;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.impl.LavaRenderHandler;
import me.valkeea.fishyaddons.vconfig.annotation.VCInit;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.fluid.Fluids;

@VCModule
public class TransLava {
    private TransLava() {}
    private static boolean enabled = false;
    private static int color = 0;
    private static FluidRenderHandler originalLavaHandler = null;
    private static FluidRenderHandler originalFlowingLavaHandler = null;

    @VCInit
    public static void init() {
        FaEvents.ENVIRONMENT_CHANGE.register(event -> update(event.newIsland(), event.isSkyblock()));
    }

    @VCListener(value = BooleanKey.TRANS_LAVA, ints = IntKey.TRANS_LAVA_COLOR)
    public static void update() {
        update(SkyblockAreas.getIsland(), GameMode.skyblock());
    }

    public static void update(Island island, boolean isSkyblock) {
        if (originalLavaHandler == null) {
            originalLavaHandler = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.LAVA);
            originalFlowingLavaHandler = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.FLOWING_LAVA);
        }

        boolean wasEnabled = enabled;
        int prevColor = color;

        enabled = isSkyblock && island.equals(Island.CI)
                && Config.get(BooleanKey.TRANS_LAVA);

        color = Config.get(IntKey.TRANS_LAVA_COLOR);

        if (wasEnabled != enabled || (enabled && prevColor != color)) {
            reloadRenderHandler();
        }
    }

    private static void reloadRenderHandler() {
        if (enabled) {
            FluidRenderHandler handler = LavaRenderHandler.customFluid("block/water", color);
            FluidRenderHandlerRegistry.INSTANCE.register(Fluids.LAVA, handler);
            FluidRenderHandlerRegistry.INSTANCE.register(Fluids.FLOWING_LAVA, handler);
            BlockRenderLayerMap.putFluids(BlockRenderLayer.TRANSLUCENT, Fluids.LAVA, Fluids.FLOWING_LAVA);
            
        } else {
            FluidRenderHandlerRegistry.INSTANCE.register(Fluids.LAVA, originalLavaHandler);
            FluidRenderHandlerRegistry.INSTANCE.register(Fluids.FLOWING_LAVA, originalFlowingLavaHandler);
            BlockRenderLayerMap.putFluids(BlockRenderLayer.SOLID, Fluids.LAVA, Fluids.FLOWING_LAVA);
        }

        WorldRenderer renderer = MinecraftClient.getInstance().worldRenderer;
        if (renderer != null) {
            renderer.reload();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getColor() {
        return color;
    }
}
