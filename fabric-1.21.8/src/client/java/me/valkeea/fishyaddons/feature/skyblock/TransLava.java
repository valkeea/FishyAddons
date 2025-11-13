package me.valkeea.fishyaddons.feature.skyblock;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas.Island;
import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.impl.LavaRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.Identifier;

public class TransLava {
    private TransLava() {}
    private static boolean isEnabled = false;
    private static int color = 0;
    private static FluidRenderHandler originalLavaHandler = null;
    private static FluidRenderHandler originalFlowingLavaHandler = null;

    public static void init() {
        FaEvents.ENVIRONMENT_CHANGE.register(event -> update(event.newIsland(), event.isSkyblock()));
    }

    public static void update() {
        update(SkyblockAreas.getIsland(), GameMode.skyblock());
    }

    public static void update(Island island, boolean isSkyblock) {
        if (originalLavaHandler == null) {
            originalLavaHandler = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.LAVA);
            originalFlowingLavaHandler = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.FLOWING_LAVA);
        }

        boolean wasEnabled = isEnabled;
        int prevColor = color;

        isEnabled = isSkyblock && island.equals(Island.CI)
                && FishyConfig.getState(Key.FISHY_TRANS_LAVA, false);

        color = FishyConfig.getInt(Key.FISHY_TRANS_LAVA_COLOR, -13700380);
        System.out.println("[FishyAddons] TransLava state changed: isEnabled=" + isEnabled + ", isSkyblock=" + isSkyblock +
            ", island=" + island);
        if (wasEnabled != isEnabled || (isEnabled && prevColor != color)) {
            reloadRenderHandler();
        }
    }

    private static void reloadRenderHandler() {
        if (isEnabled) {
            FluidRenderHandler handler = LavaRenderHandler.coloredFluid(color);
            
            var atlasId = Identifier.ofVanilla("textures/atlas/blocks.png");
            var atlasTexture = MinecraftClient.getInstance().getBakedModelManager().getAtlas(atlasId);
            handler.reloadTextures(atlasTexture);

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
        return isEnabled;
    }

    public static int getColor() {
        return color;
    }
}
