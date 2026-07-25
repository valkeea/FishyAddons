package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.GameMessageEvent;
import me.valkeea.fishyaddons.event.impl.GuiChangeEvent;
import me.valkeea.fishyaddons.feature.qol.NetworkMetrics;
import me.valkeea.fishyaddons.feature.skyblock.CatchAlert;
import me.valkeea.fishyaddons.tracker.profit.InventoryTracker;
import me.valkeea.fishyaddons.util.TabScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;

@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {

    @Inject(
        method = "handlePongResponse",
        at = @At("HEAD")
    )
    private void onPingResult(ClientboundPongResponsePacket packet, CallbackInfo ci) {
        NetworkMetrics.onPingResponse(packet);
    }

    @Inject(
        method = "handlePlayerInfoUpdate",
        at = @At("TAIL")
    )
    private void onTab(ClientboundPlayerInfoUpdatePacket packet, CallbackInfo ci) {
        TabScanner.onUpdate(packet);
    }     
    
    @Inject(
        method = "handleContainerSetSlot",
        at = @At("TAIL")
    )
    private void onSlotUpdate(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (packet.getContainerId() == 0) {
            int slotId = packet.getSlot();
            var stack = packet.getItem();
            
            if (!stack.isEmpty() && (slotId < 5 || slotId > 8)) {
                InventoryTracker.onItemAdded(stack);
            }
        }
    }

    @Inject(
        method = "handleContainerContent",
        at = @At("TAIL")
    )
    private void inventory(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        var screen = McApi.screen();
        if (!(screen instanceof ContainerScreen cs)) return;

        var title = cs.getTitle();

        var event = new GuiChangeEvent(cs, title);
        FaEvents.GUI_CHANGE.firePhased(event, listener -> listener.onGuiChange(event));
    }

    @Inject(
        method = "handleSystemChat",
        at = @At("HEAD")
    )
    private void passRaw(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (packet == null || !Minecraft.getInstance().isSameThread()) return;

        var pristine = packet.content();
        GameMessageEvent event = new GameMessageEvent(pristine, packet.overlay());
        FaEvents.GAME_MESSAGE.firePhased(event, listener -> listener.onGameMessage(event));
    }

    @Inject(
        method = "handleSoundEvent",
        at = @At("HEAD")
    )
	public void onPlaySound(ClientboundSoundPacket packet, CallbackInfo ci) {
        var sound = packet.getSound().value();
        var soundId = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getKey(sound);
        
        if (soundId != null) {
            CatchAlert.recordPitch(soundId.toString(), packet.getPitch());
        }
    }
}
