package me.valkeea.fishyaddons.mixin;

import me.valkeea.fishyaddons.handler.ItemSearchOverlay;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class MixinHandledScreenSearchInput {
    
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (ItemSearchOverlay.getInstance().handleKeyPressed(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        }
    }
    
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (ItemSearchOverlay.getInstance().handleMouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }
}
