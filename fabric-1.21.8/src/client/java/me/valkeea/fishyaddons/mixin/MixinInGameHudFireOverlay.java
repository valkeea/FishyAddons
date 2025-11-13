package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import me.valkeea.fishyaddons.feature.visual.RenderTweaks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;

/**
 * For now, returns a soul fire sprite which is replaced with a rsp
 * (only used for one block on Galatea but eventually should be changed to a custom texture)
 */
@SuppressWarnings("squid:S1118")
@Mixin(InGameOverlayRenderer.class)
public class MixinInGameHudFireOverlay {

    @ModifyVariable(
        method = "renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
        at = @At(value = "STORE"),
        ordinal = 0
    )
    private static Sprite redirectFireSprite(Sprite originalSprite) {
        if (RenderTweaks.tryColorFire() != 0) {
            var atlasId = Identifier.ofVanilla("textures/atlas/blocks.png");
            var atlas = MinecraftClient.getInstance().getBakedModelManager().getAtlas(atlasId);
            var sprite = atlas.getSprite(Identifier.of("minecraft", "block/soul_fire_0"));

            if (sprite != null) return sprite;
        }
        return originalSprite;
    }

    @ModifyArgs(
        method = "renderFireOverlay(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
        at = @At(value = "INVOKE", 
                 target = "Lnet/minecraft/client/render/VertexConsumer;color(FFFF)Lnet/minecraft/client/render/VertexConsumer;")
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
