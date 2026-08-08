package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.api.skyblock.GameMode;
import me.valkeea.fishyaddons.feature.qol.NetworkMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;

@Mixin(ClientCommonPacketListenerImpl.class)
public class MixinClientCommonNetworkHandler {

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void onInit(Minecraft mc, Connection connection, CommonListenerCookie cookie, CallbackInfo ci) {
        var data = cookie.serverData();
        GameMode.checkHypixel(data != null ? data.ip : "");
    }

    @Inject(
        method = "handleKeepAlive(Lnet/minecraft/network/protocol/common/ClientboundKeepAlivePacket;)V",
        at = @At("HEAD")
    )
    private void onKeepAlive(ClientboundKeepAlivePacket packet, CallbackInfo ci) {
        NetworkMetrics.onKaS2C();       
    }
}
