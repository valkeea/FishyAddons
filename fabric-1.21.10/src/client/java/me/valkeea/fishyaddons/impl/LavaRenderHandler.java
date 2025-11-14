package me.valkeea.fishyaddons.impl;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.util.SpriteUtil;
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
    private final int tint;
    
    private Sprite cachedStillSprite;
    private boolean spritesLoaded = false;

    public LavaRenderHandler(Identifier stillTexture, int tint) {
        this.stillTexture = stillTexture;
        this.tint = tint;
    }

    /**
     * Create a handler for vanilla water texture with custom tint
     */
    public static LavaRenderHandler coloredFluid(int tint) {
        return new LavaRenderHandler(WATER_STILL, tint);
    }
    
    /**
     * Create a handler with a custom texture and tint.
     */
    public static LavaRenderHandler customFluid(String modId, String texturePath, int tint) {
        return new LavaRenderHandler(SpriteUtil.createModSprite(modId, texturePath), tint);
    }
    
    private void loadSprites() {
        if (spritesLoaded) return;
        
        try {
            cachedStillSprite = SpriteUtil.getBlockSprite(stillTexture);
            
            if (cachedStillSprite != null) {
                spritesLoaded = true;
            }
        } catch (Exception e) {
            System.err.println("[FishyAddons] Failed to load sprite: " + stillTexture + " - " + e.getMessage());
        }
    }

    @Override
    public Sprite[] getFluidSprites(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {

        if (spritesLoaded && cachedStillSprite != null) {
            return new Sprite[] { cachedStillSprite, cachedStillSprite };
        }
        
        if (!spritesLoaded) {
            loadSprites();
            if (cachedStillSprite != null) {
                return new Sprite[] { cachedStillSprite, cachedStillSprite };
            }
        }
        
        Sprite lavaStill = SpriteUtil.getBlockSprite(WATER_STILL);
        if (lavaStill != null) {
            cachedStillSprite = lavaStill;
            return new Sprite[] { lavaStill, lavaStill };
        }
        
        System.err.println("[FishyAddons] CRITICAL: All sprite loading methods failed, returning null sprite array");
        return new Sprite[] { null, null };
    }

    @Override
    public void reloadTextures(SpriteAtlasTexture atlas) {

        try {
            cachedStillSprite = atlas.getSprite(stillTexture);
            spritesLoaded = true;

        } catch (Exception e) {

            try {
                cachedStillSprite = atlas.getSprite(WATER_STILL);
                spritesLoaded = true;

            } catch (Exception e2) {
                spritesLoaded = false;
                cachedStillSprite = null;
            }
        }
    }

    @Override
    public int getFluidColor(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {
        return tint;
    }
}
