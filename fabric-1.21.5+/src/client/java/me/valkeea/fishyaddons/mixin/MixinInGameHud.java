package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import me.valkeea.fishyaddons.handler.XpColor;
import net.minecraft.client.gui.hud.InGameHud;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    @ModifyArg(
        method = "renderExperienceLevel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I",
            ordinal = 4
        ),
        index = 4
    )
    private int modifyXPTextColor(int color) {
        if (XpColor.isEnabled()) {
            return XpColor.get();
        }
        return color;
    }
}