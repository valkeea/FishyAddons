package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.HudRenderEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    
    @Shadow
    private MinecraftClient client;

    @Inject(
        method = "render",
        at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/hud/InGameHud;renderPlayerList(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
        shift = At.Shift.BEFORE)
    )
    private void onHudRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        
        var event = new HudRenderEvent(context, tickCounter, client, false);
        FaEvents.HUD_RENDER.firePhased(event, listener -> listener.onHudRender(event));
    }
}
