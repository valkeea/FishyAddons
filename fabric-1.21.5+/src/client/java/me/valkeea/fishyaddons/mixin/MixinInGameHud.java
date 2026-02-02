package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.HudRenderEvent;
import me.valkeea.fishyaddons.feature.visual.XpColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onHudRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().options.hudHidden) {
            var event = new HudRenderEvent(context, tickCounter);
            FaEvents.HUD_RENDER.firePhased(event, listener -> listener.onHudRender(event));
        }
    }
    
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
