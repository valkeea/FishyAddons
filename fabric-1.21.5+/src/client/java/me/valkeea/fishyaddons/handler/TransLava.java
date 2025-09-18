package me.valkeea.fishyaddons.handler;

import me.valkeea.fishyaddons.config.FishyConfig;
import me.valkeea.fishyaddons.config.Key;
import me.valkeea.fishyaddons.impl.LavaRenderHandler;
import me.valkeea.fishyaddons.util.AreaUtils;
import me.valkeea.fishyaddons.util.SkyblockCheck;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.Identifier;

public class TransLava {
    private TransLava() {}
    private static boolean isEnabled = false;
    private static int color = 0;
    private static FluidRenderHandler originalLavaHandler = null;
    private static FluidRenderHandler originalFlowingLavaHandler = null;

    public static void update() {
        if (originalLavaHandler == null) {
            originalLavaHandler = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.LAVA);
            originalFlowingLavaHandler = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.FLOWING_LAVA);
        }

        boolean wasEnabled = isEnabled;
        int prevColor = color;

        isEnabled = SkyblockCheck.getInstance().rules() && AreaUtils.isCrimson()
                && FishyConfig.getState(Key.FISHY_TRANS_LAVA, false);

        color = FishyConfig.getInt(Key.FISHY_TRANS_LAVA_COLOR, -13700380);

        if (wasEnabled != isEnabled || (isEnabled && prevColor != color)) {
            reloadRenderHandler();
        }
    }

    private static void reloadRenderHandler() {
        if (isEnabled) {
            FluidRenderHandler handler = LavaRenderHandler.coloredFluid(color);
            
            var atlasId = Identifier.ofVanilla("textures/atlas/blocks.png");
            SpriteAtlasTexture texture = MinecraftClient.getInstance().getBakedModelManager().getAtlas(atlasId);
            handler.reloadTextures(texture);

            FluidRenderHandlerRegistry.INSTANCE.register(Fluids.LAVA, handler);
            FluidRenderHandlerRegistry.INSTANCE.register(Fluids.FLOWING_LAVA, handler);
            BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), Fluids.LAVA, Fluids.FLOWING_LAVA);
            
        } else {
            FluidRenderHandlerRegistry.INSTANCE.register(Fluids.LAVA, originalLavaHandler);
            FluidRenderHandlerRegistry.INSTANCE.register(Fluids.FLOWING_LAVA, originalFlowingLavaHandler);
            BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getSolid(), Fluids.LAVA, Fluids.FLOWING_LAVA);
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