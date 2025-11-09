package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.qol.NetworkMetrics;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;

@Mixin(ClientCommonNetworkHandler.class)
public class MixinClientCommonNetworkHandler {

    @Inject(
        method = "onKeepAlive(Lnet/minecraft/network/packet/s2c/common/KeepAliveS2CPacket;)V",
        at = @At("HEAD")
    )
    private void onKeepAlive(KeepAliveS2CPacket packet, CallbackInfo ci) {
        NetworkMetrics.onKaS2C();       
    }
}
