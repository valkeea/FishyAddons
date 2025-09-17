package me.valkeea.fishyaddons.mixin;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.handler.XpColor;
import me.valkeea.fishyaddons.util.text.TextUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

@Mixin(InGameHud.class)
public class MixinInGameHudOutline {

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient c = MinecraftClient.getInstance();
        PlayerEntity plr = c.player;
        
        if (plr == null || plr.experienceLevel <= 0 || 
        !me.valkeea.fishyaddons.handler.XpColor.isOutlineEnabled()) return;

        String levelText = String.valueOf(plr.experienceLevel);
        TextRenderer tr = c.textRenderer;

        int textWidth = tr.getWidth(levelText);
        int x = (context.getScaledWindowWidth() - textWidth) / 2;
        int y = context.getScaledWindowHeight() - 31 - 4;

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        VertexConsumerProvider vertices = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        TextUtils.drawOutlinedText(
            context,
            tr,
            Text.literal(levelText),
            x, y,
            XpColor.get(),
            0x000000,
            matrix,
            vertices,
            TextRenderer.TextLayerType.NORMAL,
            0xF000F0 
        );

        ci.cancel();
    }
}