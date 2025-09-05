package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.listener.ScreenChange;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;

@Mixin(HandledScreen.class)
public class MixinHandledScreenChange {
    
    @Inject(method = "init", at = @At("HEAD"))
    private void onScreenInit(CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        Text title = screen.getTitle();
        if (title != null) {
            ScreenChange.onInit(screen, title);
        }
    }
    
    @Inject(method = "close", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        Text title = screen.getTitle();
        if (title != null) {
            ScreenChange.onClose(title);
        }
    }
}
