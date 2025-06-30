package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.handler.ClientPing;
import me.valkeea.fishyaddons.handler.PetInfo;
import me.valkeea.fishyaddons.handler.TabScanner;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;


@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Inject(method = "onPingResult", at = @At("HEAD"))
    private void onPingResult(PingResultS2CPacket packet, CallbackInfo ci) {
        ClientPing.onPingResponse(packet);
    }

    @Inject(
        method = "onPlayerList",
        at = @At("TAIL")
    )
    private void ons2c(PlayerListS2CPacket packet, CallbackInfo ci) {
        if (!PetInfo.isOn() || !me.valkeea.fishyaddons.util.SkyblockCheck.getInstance().rules()) return;
        if (PetInfo.getNextCheck()) {
            TabScanner.onUpdate();
        }
    }    
}