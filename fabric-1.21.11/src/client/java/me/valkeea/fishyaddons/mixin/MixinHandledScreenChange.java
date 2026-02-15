package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.GuiOpenEvent;
import me.valkeea.fishyaddons.event.impl.GuiCloseEvent;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.text.Text;

@Mixin(HandledScreen.class)
public class MixinHandledScreenChange {
    
    @Inject(method = "init", at = @At("HEAD"))
    private void onScreenInit(CallbackInfo ci) {

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        if (!(screen instanceof GenericContainerScreen gcs)) return;
        
        Text t = gcs.getTitle();
            if (t != null) {
                var event = new GuiOpenEvent(gcs, t);
                FaEvents.GUI_OPEN.firePhased(event, listener -> listener.onScreenOpen(event));
            }
    }
    
    @Inject(method = "close", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        if (!(screen instanceof GenericContainerScreen gcs)) return;
        
        Text t = gcs.getTitle();
        if (t != null) {
            var event = new GuiCloseEvent(t);
            FaEvents.GUI_CLOSE.firePhased(event, listener -> listener.onScreenClose(event));
        }
    }
}
