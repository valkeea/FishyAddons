package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import me.valkeea.fishyaddons.feature.visual.RenderTweaks;
import me.valkeea.fishyaddons.util.SpriteUtil;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@SuppressWarnings("squid:S1118")
@Mixin(ScreenEffectRenderer.class)
public class MixinInGameHudFireOverlay {

    @ModifyVariable(
        method = "submitFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
        at = @At("HEAD"),
        ordinal = 0
    )
    private static TextureAtlasSprite redirectFireSprite(TextureAtlasSprite originalSprite) {

        int tint = RenderTweaks.tryColorFire();
        if (tint != 0) {
            var sprite = SpriteUtil.getModBlockSprite("block/fire");
            if (sprite != null) return sprite;
        }
        return originalSprite;
    }

    @ModifyConstant(
        method = "buildFireQuad(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;)V",
        constant = @Constant(intValue = -436207617)
    )
    private static int addOverlayTint(int originalColor) {

        int tint = RenderTweaks.tryColorFire();
        if (tint != 0) {
            return (originalColor & 0xFF000000) | (tint & 0x00FFFFFF);
        }
        return originalColor;
    }
}
