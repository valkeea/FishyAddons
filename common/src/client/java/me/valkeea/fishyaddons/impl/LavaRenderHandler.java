package me.valkeea.fishyaddons.impl;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

/**
 * Custom fluid render handler to bypass biome blending.
 * Flow animation can implemented but not needed for this use case.
 */
public class LavaRenderHandler implements FluidRenderHandler {
    public static final Identifier WATER_STILL = Identifier.ofVanilla("block/water_still");

    private final Identifier stillTexture;
    private final Sprite[] sprites;
    private final int tint;

    public LavaRenderHandler(Identifier stillTexture, int tint) {
        this.stillTexture = stillTexture;
        this.sprites = new Sprite[2];
        this.tint = tint;
    }

    public static LavaRenderHandler coloredFluid(int tint) {
        return new LavaRenderHandler(WATER_STILL, tint);
    }

    @Override
    public Sprite[] getFluidSprites(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {
        return new Sprite[] { sprites[0], sprites[0] };
    }

    @Override
    public void reloadTextures(SpriteAtlasTexture atlas) {
        sprites[0] = atlas.getSprite(stillTexture);
        sprites[1] = sprites[0];
    }

    @Override
    public int getFluidColor(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {
        return tint;
    }
}
