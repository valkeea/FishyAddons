package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import me.valkeea.fishyaddons.feature.visual.RenderTweaks;
import me.valkeea.fishyaddons.util.SpriteUtil;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@SuppressWarnings("squid:S1118")
@Mixin(ScreenEffectRenderer.class)
public class MixinInGameHudFireOverlay {

    @ModifyVariable(
        method = "renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
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

    @ModifyArgs(
        method = "renderFire(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private static void addOverlayTint(Args args) {

        int tint = RenderTweaks.tryColorFire();
        if (tint != 0) {

            float newRed = ((tint >> 16) & 0xFF) / 255.0f;
            float newGreen = ((tint >> 8) & 0xFF) / 255.0f;
            float newBlue = (tint & 0xFF) / 255.0f;

            args.set(0, newRed);
            args.set(1, newGreen);
            args.set(2, newBlue);
        }
    }
}
