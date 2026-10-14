package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import me.valkeea.fishyaddons.api.skyblock.SkyblockAreas;
import me.valkeea.fishyaddons.feature.visual.ato.AreaTextureOverrides;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(targets = "net.minecraft.client.particle.ParticleResources$MutableSpriteSet")
public class MixinParticleSpriteSet {

    @ModifyReturnValue(
        method = "get(II)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;",
        at = @At("RETURN")
    )
    private TextureAtlasSprite remapByAge(TextureAtlasSprite original) {
        return remap(original);
    }

    @ModifyReturnValue(
        method = "get(Lnet/minecraft/util/RandomSource;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;",
        at = @At("RETURN")
    )
    private TextureAtlasSprite remapByRandom(TextureAtlasSprite original) {
        return remap(original);
    }

    @ModifyReturnValue(method = "first", at = @At("RETURN"))
    private TextureAtlasSprite remapFirst(TextureAtlasSprite original) {
        return remap(original);
    }

    private static TextureAtlasSprite remap(TextureAtlasSprite original) {
        var remapped = AreaTextureOverrides.remap(SkyblockAreas.getIsland(), original.contents().name());
        if (remapped == null) return original;

        if (Minecraft.getInstance().getTextureManager().getTexture(original.atlasLocation()) instanceof TextureAtlas atlas) {
            return atlas.getSprite(remapped);
        }
        return original;
    }
}
