package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.ScreenClickEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;

@Mixin(AbstractContainerScreen.class)
public class MixinHandledScreenMouseClicked {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {

        var event = new ScreenClickEvent((AbstractContainerScreen<?>)(Object)this, ((HandledScreenAccessor)this).getHoveredSlot(), click, doubled);
        Boolean result = FaEvents.SCREEN_MOUSE_CLICK.fireReturnable(event, listener -> listener.onClick(event), false);

        if (event.isConsumed()) cir.setReturnValue(result);
    } 
}
