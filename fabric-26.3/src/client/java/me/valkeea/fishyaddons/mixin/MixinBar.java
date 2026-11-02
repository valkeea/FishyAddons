package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.visual.XpColor;
import me.valkeea.fishyaddons.hud.core.HudDrawer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.network.chat.Component;

@Mixin(ContextualBar.class)
public interface MixinBar {
    
    @Inject(
        method = "extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onDrawExperienceLevel(GuiGraphicsExtractor gge, Font font, int level, CallbackInfo ci) {
        if (XpColor.isEnabled() && XpColor.isOutlineEnabled()) {
            var text = Component.translatable("gui.experience.level", level);
            int x = (gge.guiWidth() - font.width(text)) / 2;
            int y = gge.guiHeight() - 24 - 9 - 2;
            HudDrawer.drawText(gge, text, x, y, XpColor.get(), true);
            ci.cancel();
        }
    }
    
    @ModifyArg(
        method = "extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
        ordinal = 4),
        index = 4
    )
    private static int modifyXPTextColor(int color) {
        if (XpColor.isEnabled() && !XpColor.isOutlineEnabled()) {
            return XpColor.get();
        }
        return color;
    }
}
