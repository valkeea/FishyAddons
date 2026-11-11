package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.item.safeguard.DropProtection;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    
    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void faDropItem(LocalPlayer player, boolean all, CallbackInfo ci) {
        if (DropProtection.blockThrow(player)) ci.cancel();
    }
}
