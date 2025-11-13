package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.HudRenderEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onHudRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        HudRenderEvent event = new HudRenderEvent(context, tickCounter);
        FaEvents.HUD_RENDER.firePhased(event, listener -> listener.onHudRender(event));
    }
}
