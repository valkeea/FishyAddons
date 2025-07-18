package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.handler.ClientPing;
import me.valkeea.fishyaddons.handler.PetInfo;
import me.valkeea.fishyaddons.handler.TabScanner;
import me.valkeea.fishyaddons.tracker.InventoryTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
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
    
    @Inject(
        method = "onScreenHandlerSlotUpdate",
        at = @At("TAIL")
    )
    private void onSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci) {
        // Check if this is the player's inventory (syncId 0) and != slot 5 (helmet)
        if (packet.getSyncId() == 0) {
            int slotId = packet.getSlot();
            net.minecraft.item.ItemStack stack = packet.getStack();
            if (!stack.isEmpty() && slotId != 5) {
                InventoryTracker.onItemAdded(stack);
            }
        }
    }
}