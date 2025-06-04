package me.wait.fishyaddons.impl;

import me.wait.fishyaddons.config.TextureConfig;
import me.wait.fishyaddons.handlers.RetexHandler;
import me.wait.fishyaddons.util.ModelUtils;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/*
 * Wraps the baked model and remaps its quads' UVs, 
 * used at runtime for each registered island
 */

@SideOnly(Side.CLIENT)
public class GhostModel implements IBakedModel {
    private final IBakedModel defaultModel;
    private final ModelResourceLocation modelLocation;
    private final String originalTexture;
    private static final Map<String, TextureAtlasSprite> spriteCache = new HashMap<>();

    public GhostModel(IBakedModel defaultModel, ModelResourceLocation modelLocation, String originalTexture) {
        this.defaultModel = defaultModel;
        this.modelLocation = modelLocation;
        this.originalTexture = originalTexture;
    }

    public static void clearSpriteCache() {
        spriteCache.clear();
    }

    private TextureAtlasSprite getIslandTexture() {
        String island = RetexHandler.getIsland();
        String modelPath = modelLocation.getResourcePath();
        String cacheKey = island + "|" + modelPath;

        if (spriteCache.containsKey(cacheKey)) {
            return spriteCache.get(cacheKey);
        }

        TextureAtlasSprite sprite = null;

        if (TextureConfig.isIslandTextureEnabled(island)) {
            String path = "fishyaddons:islands/" + island + "/" + modelPath;
            sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(path);

            if (sprite != null && sprite != Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite()) {
                spriteCache.put(cacheKey, sprite);
                return sprite;
            } 
        }

        sprite = getOgTexture();
        spriteCache.put(cacheKey, sprite);
        return sprite;
    }

    private TextureAtlasSprite getOgTexture() {
        if (originalTexture != null && !originalTexture.isEmpty()) {
            return Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(originalTexture);
        }
        return defaultModel.getParticleTexture();
    }

    private List<BakedQuad> remapQuads(List<BakedQuad> quads) {
        TextureAtlasSprite newSprite = getIslandTexture();
        TextureAtlasSprite oldSprite = getOgTexture();
        if (newSprite == null || oldSprite == null) return quads;

        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            result.add(remapQuad(quad, oldSprite, newSprite));
        }
        return result;
    }

    private List<BakedQuad> remapQuads(List<BakedQuad> quads, EnumFacing fallbackFace) {
        String blockName = modelLocation.getResourcePath();
        for (Map.Entry<String, String[]> entry : ModelUtils.VARIANT_SUFFIXES.entrySet()) {
            if (blockName.contains(entry.getKey())) {
                String[] suffixes = entry.getValue();
                List<BakedQuad> result = new ArrayList<>(quads.size());
                for (BakedQuad quad : quads) {
                    EnumFacing actualFace = quad.getFace() != null ? quad.getFace() : fallbackFace;
                    String texName = ModelUtils.getVariantTextureName(blockName, entry.getKey(), suffixes, actualFace);
                    String base = "fishyaddons:islands/" + RetexHandler.getIsland() + "/";
                    String fallbackBase = "minecraft:blocks/";
                    TextureAtlasSprite from = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fallbackBase + texName);
                    TextureAtlasSprite to = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(base + texName);
                    if (to == null || to == Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite()) {
                        to = from;
                    }
                    if (from == null || from == Minecraft.getMinecraft().getTextureMapBlocks().getMissingSprite()) {
                        result.add(quad);
                    } else {
                        result.add(remapQuad(quad, from, to));
                    }
                }
                return result;
            }
        }
        return remapQuads(quads);
    }


    private float getUnInterpolatedU(TextureAtlasSprite sprite, float u) {
        float scale = 16.0F / (sprite.getMaxU() - sprite.getMinU());
        return (u - sprite.getMinU()) * scale;
    }

    private float getUnInterpolatedV(TextureAtlasSprite sprite, float v) {
        float scale = 16.0F / (sprite.getMaxV() - sprite.getMinV());
        return (v - sprite.getMinV()) * scale;
    }

    private boolean needsImprovedRemap() {
        String path = modelLocation.getResourcePath();
        return path.contains("pane") || path.contains("sandstone_slab");
    }

    private BakedQuad remapQuad(BakedQuad quad, TextureAtlasSprite from, TextureAtlasSprite to) {
        if (!needsImprovedRemap()) {
            // shader compatible
            int[] oldData = quad.getVertexData();
            int[] newData = oldData.clone();
            int stride = oldData.length / 4;
            for (int i = 0; i < 4; i++) {
                int base = i * stride;
                float u = Float.intBitsToFloat(oldData[base + 4]);
                float v = Float.intBitsToFloat(oldData[base + 5]);
                float un = getUnInterpolatedU(from, u);
                float vn = getUnInterpolatedV(from, v);
                float newU = to.getInterpolatedU(un);
                float newV = to.getInterpolatedV(vn);
                newData[base + 4] = Float.floatToRawIntBits(newU);
                newData[base + 5] = Float.floatToRawIntBits(newV);
            }
            return new BakedQuad(newData, quad.getTintIndex(), quad.getFace());
        } else {
            // manual remapping logic for slabs/panes
            int[] oldData = quad.getVertexData();
            int[] newData = oldData.clone();
            int stride = oldData.length / 4;
            if (stride < 6 || stride > 8) {
                return quad;
            }
            float minU = Float.POSITIVE_INFINITY, maxU = Float.NEGATIVE_INFINITY;
            float minV = Float.POSITIVE_INFINITY, maxV = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < 4; i++) {
                int base = i * stride;
                float u = Float.intBitsToFloat(oldData[base + 4]);
                float v = Float.intBitsToFloat(oldData[base + 5]);
                minU = Math.min(minU, u);
                maxU = Math.max(maxU, u);
                minV = Math.min(minV, v);
                maxV = Math.max(maxV, v);
            }
            float scaleU = (maxU - minU) == 0 ? 0 : 16.0f / (maxU - minU);
            float scaleV = (maxV - minV) == 0 ? 0 : 16.0f / (maxV - minV);
            for (int i = 0; i < 4; i++) {
                int base = i * stride;
                float u = Float.intBitsToFloat(oldData[base + 4]);
                float v = Float.intBitsToFloat(oldData[base + 5]);
                float un = (u - minU) * scaleU;
                float vn = (v - minV) * scaleV;
                un = Math.round(un * 16f) / 16f;
                vn = Math.round(vn * 16f) / 16f;
                un = Math.max(0f, Math.min(15.9999f, un));
                vn = Math.max(0f, Math.min(15.9999f, vn));
                float newU = to.getInterpolatedU(un);
                float newV = to.getInterpolatedV(vn);
                if (Float.isNaN(newU) || Float.isNaN(newV) || Float.isInfinite(newU) || Float.isInfinite(newV)) {
                    return quad;
                }
                newData[base + 4] = Float.floatToRawIntBits(newU);
                newData[base + 5] = Float.floatToRawIntBits(newV);
            }
            return new BakedQuad(newData, quad.getTintIndex(), quad.getFace());
        }
    }


    @Override public List<BakedQuad> getFaceQuads(EnumFacing side) { return remapQuads(defaultModel.getFaceQuads(side), side); }
    @Override public List<BakedQuad> getGeneralQuads() { return remapQuads(defaultModel.getGeneralQuads()); }
    @Override public boolean isAmbientOcclusion() { return defaultModel.isAmbientOcclusion(); }
    @Override public boolean isGui3d() { return defaultModel.isGui3d(); }
    @Override public boolean isBuiltInRenderer() { return defaultModel.isBuiltInRenderer(); }
    @Override public TextureAtlasSprite getParticleTexture() { return getIslandTexture(); }
    @Override public ItemCameraTransforms getItemCameraTransforms() { return defaultModel.getItemCameraTransforms(); }
}