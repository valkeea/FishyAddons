package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.qol.NetworkMetrics;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;

@Mixin(ClientCommonPacketListenerImpl.class)
public class MixinClientCommonNetworkHandler {

    @Inject(
        method = "handleKeepAlive(Lnet/minecraft/network/protocol/common/ClientboundKeepAlivePacket;)V",
        at = @At("HEAD")
    )
    private void onKeepAlive(ClientboundKeepAlivePacket packet, CallbackInfo ci) {
        NetworkMetrics.onKaS2C();       
    }
}
