package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import me.valkeea.fishyaddons.feature.visual.RenderTweaks;
import me.valkeea.fishyaddons.util.SpriteUtil;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.texture.Sprite;

@SuppressWarnings("squid:S1118")
@Mixin(InGameOverlayRenderer.class)
public class MixinInGameHudFireOverlay {

    @ModifyVariable(
        method = "renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/texture/Sprite;)V",
        at = @At("HEAD"),
        ordinal = 0
    )
    private static Sprite redirectFireSprite(Sprite originalSprite) {

        int tint = RenderTweaks.tryColorFire();
        if (tint != 0) {
            var sprite = SpriteUtil.getModBlockSprite("fishyaddons", "block/fire");
            if (sprite != null) return sprite;
        }
        return originalSprite;
    }

    @ModifyArgs(
        method = "renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/texture/Sprite;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumer;color(FFFF)Lnet/minecraft/client/render/VertexConsumer;")
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
