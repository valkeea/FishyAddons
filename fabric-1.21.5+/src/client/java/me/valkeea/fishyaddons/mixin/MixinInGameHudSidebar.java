package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import me.valkeea.fishyaddons.handler.FaColors;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;

@Mixin(value = InGameHud.class, priority = 1500)
public class MixinInGameHudSidebar {

    @Redirect(
        method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"
        )
    )
    private int recolorSidebarString(DrawContext ctx, TextRenderer tr, Text text, int x, int y, int color, boolean shadow) {
        Text recolored = FaColors.recolorSidebarText(text);
        return ctx.drawText(tr, recolored, x, y, color, shadow);
    }
}