package me.valkeea.fishyaddons.util;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.Identifier;

public class SpriteUtil {
    private SpriteUtil() {}
    private static final String MODID = "fishyaddons";

    @Nullable
    private static SpriteAtlasTexture getBlockAtlas() {
        var client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        
        try {
            if (client.getTextureManager() != null) {
                return tryGetAtlasFromTextureManager(client);
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Nullable
    private static SpriteAtlasTexture tryGetAtlasFromTextureManager(MinecraftClient client) {
        try {
            Object texture = client.getTextureManager().getTexture(Identifier.ofVanilla("textures/atlas/blocks.png"));
            if (texture instanceof SpriteAtlasTexture sat) {
                return sat;
            }
        } catch (Exception e) {
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
    public static Sprite getBlockSprite(Identifier spriteId) {
        
        var blockAtlas = getBlockAtlas();
        if (blockAtlas == null) {
            return null;
        }
        
        try {
            var sprite = blockAtlas.getSprite(spriteId);
            if (sprite != null) return sprite;

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
    public static Sprite getBlockSpriteOrMissing(Identifier spriteId, @Nullable SpriteAtlasTexture atlas) {
        var sprite = getBlockSprite(spriteId);
        if (sprite != null) {
            return sprite;
        }
        
        if (atlas != null) {
            try {
                return atlas.getMissingSprite();
            } catch (Exception e) {
                // Atlas not initialized
            }
        }
        
        var client = MinecraftClient.getInstance();
        if (client != null && client.getAtlasManager() != null) {
            try {
                var missingId = new SpriteIdentifier(
                    Identifier.ofVanilla("textures/atlas/blocks"),
                    Identifier.ofVanilla("missingno")
                );
                return client.getAtlasManager().getSprite(missingId);

            } catch (Exception e) {
                // Even missing sprite failed
            }
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
        return Identifier.of(MODID, "/textures/" + texturePath + ".png");
    }  
    
    /**
     * Get a custom mod sprite from the block atlas
     * 
     * @param texturePath The path relative to mod textures/ without extension
     * @return The sprite, or null if not found
     */
    @Nullable
    public static Sprite getModBlockSprite(String texturePath) {
        return getBlockSprite(Identifier.of(MODID, texturePath));
    }
}
