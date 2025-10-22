package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.handler.NetworkMetrics;
import me.valkeea.fishyaddons.processor.ChatProcessor;
import me.valkeea.fishyaddons.tracker.InventoryTracker;
import me.valkeea.fishyaddons.util.SbGui;
import me.valkeea.fishyaddons.util.TabScanner;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Inject(
        method = "onPingResult",
        at = @At("HEAD")
    )
    private void onPingResult(PingResultS2CPacket packet, CallbackInfo ci) {
        NetworkMetrics.onPingResponse(packet);
    }

    @Inject(
        method = "onPlayerList",
        at = @At("TAIL")
    )
    private void onTab(PlayerListS2CPacket packet, CallbackInfo ci) {
        TabScanner.onUpdate(packet);
    }     
    
    @Inject(
        method = "onScreenHandlerSlotUpdate",
        at = @At("TAIL")
    )
    private void onSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        if (packet.getSyncId() == 0) {
            int slotId = packet.getSlot();
            var stack = packet.getStack();

            if (!stack.isEmpty() && (slotId < 5 || slotId > 8)) {
                InventoryTracker.onItemAdded(stack);
            }
        }
    }

    @Inject(
        method = "onInventory",
        at = @At("TAIL")
    )
    private void inventory(InventoryS2CPacket packet, CallbackInfo ci) {
        SbGui.getInstance().onInvUpdate();
    }

    @Inject(method = "onGameMessage",
    at = @At("HEAD")
    )
    private void passGuildRaw(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (packet == null) return;
        if (packet.overlay()) return;

        var pristine = packet.content();
        ChatProcessor.onRaw(pristine);
    }
}