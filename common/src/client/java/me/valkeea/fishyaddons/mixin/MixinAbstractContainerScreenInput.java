package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.event.impl.ContainerClickEvent;
import me.valkeea.fishyaddons.event.impl.ContainerKeyInputEvent;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.feature.item.safeguard.DropProtection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreenInput {

    @Inject(
        method = "keyPressed",
        at = @At("HEAD"), cancellable = true
    )
    private void faKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || !McApi.screenIsActive()) return;

        var gui = (AbstractContainerScreen<?>) (Object) this;
        var event = new ContainerKeyInputEvent(mc, gui, ((AbstractContainerScreenAccessor) gui).getHoveredSlot(), input);
        Boolean result = FaEvents.CONTAINER_KEY_INPUT.fire(event, false);
        
        if (event.isConsumed()) cir.setReturnValue(result);
    }

    @Inject(
        method = "mouseClicked",
        at = @At("HEAD"), cancellable = true
    )
    private void faMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        var event = new ContainerClickEvent((AbstractContainerScreen<?>)(Object)this, ((AbstractContainerScreenAccessor)this).getHoveredSlot(), click, doubled);
        Boolean result = FaEvents.CONTAINER_CLICK.fire(event, false);
        if (event.isConsumed()) cir.setReturnValue(result);
    }

    @Inject(
        method = "mouseReleased",
        at = @At("HEAD"), cancellable = true
    )
    private void faMouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var stack = mc.player.containerMenu.getCarried();
        if (stack.isEmpty()) return;

        var screen = (AbstractContainerScreen<?>) (Object) this;
        if (DropProtection.blockDrag(click, screen, stack)) cir.setReturnValue(false);
    }
}
