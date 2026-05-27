package me.valkeea.fishyaddons.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

public class SpriteUtil {
    private SpriteUtil() {}
    private static final String MODID = "fishyaddons";

    @Nullable
    private static TextureAtlas getBlockAtlas() {
        try {
            var texture = Minecraft.getInstance().getTextureManager().getTexture(
                Identifier.withDefaultNamespace("textures/atlas/blocks.png"));

            if (texture instanceof TextureAtlas atlas) {
                return atlas;
            }
        } catch (Exception _) {
            // Ignore
        }

        return null;
    }
    
    /**
     * Get a sprite from the block atlas using direct atlas access
     * 
     * @param spriteId The identifier of the sprite
     * (e.g., "minecraft:block/water_still" or "fishyaddons:block/custom_water")
     * 
     * @return The sprite, or null if not found
     */
    @Nullable
    public static TextureAtlasSprite getBlockSprite(Identifier spriteId) {
        var blockAtlas = getBlockAtlas();
        if (blockAtlas == null) return null;
        
        try {
            return blockAtlas.getSprite(spriteId);
        } catch (Exception e) {
            System.err.println("[FishyAddons] Failed to load sprite from atlas: " + spriteId + " - " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get a sprite from the block atlas, with fallback to missing sprite
     * 
     * @param spriteId The identifier of the sprite
     * @param atlas The atlas to use for fallback (can be null)
     * @return The sprite, or missing sprite as fallback
     */
    public static TextureAtlasSprite getBlockSpriteOrMissing(Identifier spriteId, @Nullable TextureAtlas atlas) {
        var sprite = getBlockSprite(spriteId);
        if (sprite != null) return sprite;
        
        if (atlas != null) {
            try {
                return atlas.missingSprite();
            } catch (Exception _) {
                // Atlas not initialized
            }
        }

        try {
            var missingId = new SpriteId(
                Identifier.withDefaultNamespace("textures/atlas/blocks.png"),
                Identifier.withDefaultNamespace("missingno"));
            return Minecraft.getInstance().getAtlasManager().get(missingId);

        } catch (Exception _) {
            // Even missing sprite failed
        }

        
        return null;
    }
    
    /**
     * Create a sprite identifier for custom mod textures
     * 
     * @param texturePath The path relative to mod textures/ (e.g., "block/water") without extension
     * @return The sprite identifier
     */
    public static Identifier createModSprite(String texturePath) {
        return Identifier.fromNamespaceAndPath(MODID, "/textures/" + texturePath + ".png");
    }  
    
    /**
     * Get a custom mod sprite from the block atlas
     * 
     * @param texturePath The path relative to mod textures/ without extension
     * @return The sprite, or null if not found
     */
    @Nullable
    public static TextureAtlasSprite getModBlockSprite(String texturePath) {
        return getBlockSprite(Identifier.fromNamespaceAndPath(MODID, texturePath));
    }
}
