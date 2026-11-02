package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.HudRenderEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Mixin(Hud.class)
public class MixinInGameHud {
    
    @Shadow
    private Minecraft minecraft;

    @Inject(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractTabList(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            shift = At.Shift.BEFORE
        )
    )
    private void onHudRender(GuiGraphicsExtractor gge, DeltaTracker dt, CallbackInfo ci) {
        var event = new HudRenderEvent(gge, dt, minecraft, false);
        FaEvents.HUD_RENDER.firePhased(event, listener -> listener.onHudRender(event));
    }
}
