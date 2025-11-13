package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.ScreenClickEvent;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

@SuppressWarnings("java:S2440")
@Mixin(HandledScreen.class)
public class MixinHandledScreenMouseClicked {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        
        ScreenClickEvent event = new ScreenClickEvent((HandledScreen<?>)(Object)this, ((HandledScreenAccessor)this).getFocusedSlot(), mouseX, mouseY, button);
        Boolean result = FaEvents.SCREEN_MOUSE_CLICK.fireReturnable(event, listener -> listener.onClick(event), false);

        if (event.isConsumed()) cir.setReturnValue(result);
    }
}