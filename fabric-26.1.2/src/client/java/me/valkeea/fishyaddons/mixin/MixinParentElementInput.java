package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.qol.ItemSearchOverlay;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;

@Mixin(ContainerEventHandler.class)
public interface MixinParentElementInput {
    
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onCharTyped(CharacterEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (this instanceof AbstractContainerScreen && ItemSearchOverlay.getInstance().handleCharTyped(input)) {
            cir.setReturnValue(true);
        }
    }
}
