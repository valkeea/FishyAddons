package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.visual.XpColor;
import me.valkeea.fishyaddons.render.OutlinedText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

@Mixin(InGameHud.class)
public class MixinInGameHudOutline {

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {

        var mc = MinecraftClient.getInstance();
        var plr = mc.player;

        if (plr == null || plr.experienceLevel <= 0 || !XpColor.isOutlineEnabled()) return;

        var levelText = String.valueOf(plr.experienceLevel);
        var tr = mc.textRenderer;

        int textWidth = tr.getWidth(levelText);
        int x = (context.getScaledWindowWidth() - textWidth) / 2;
        int y = context.getScaledWindowHeight() - 31 - 4;

        OutlinedText.withColor(
            context,
            tr,
            Text.literal(levelText),
            x, y,
            XpColor.get()
        );

        ci.cancel();
    }
}
