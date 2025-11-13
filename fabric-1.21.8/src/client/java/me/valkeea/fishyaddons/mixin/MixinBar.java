package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.visual.XpColor;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.text.Text;

@Mixin(Bar.class)
public interface MixinBar {
    
    @Inject(
        method = "drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onDrawExperienceLevel(DrawContext context, TextRenderer textRenderer, int level, CallbackInfo ci) {
        if (XpColor.isEnabled() && XpColor.isOutlineEnabled()) {
            Text text = Text.translatable("gui.experience.level", level);
            int x = (context.getScaledWindowWidth() - textRenderer.getWidth(text)) / 2;
            int y = context.getScaledWindowHeight() - 24 - 9 - 2;
            HudDrawer.drawText(context, text, x, y, XpColor.get(), true);
            ci.cancel();
        }
    }
    
    @ModifyArg(
        method = "drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V", ordinal = 4),
        index = 4
    )
    private static int modifyXPTextColor(int color) {
        if (XpColor.isEnabled() && !XpColor.isOutlineEnabled()) {
            return XpColor.get();
        }
        return color;
    }
}