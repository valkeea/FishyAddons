package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.item.safeguard.DropProtection;
import net.minecraft.client.player.LocalPlayer;

@Mixin(LocalPlayer.class)
public class MixinLocalPlayer {
    
    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void faDrop(boolean all, CallbackInfoReturnable<Boolean> cir) {
        if (DropProtection.blockThrow((LocalPlayer) (Object) this)) cir.setReturnValue(false);
    }
}
