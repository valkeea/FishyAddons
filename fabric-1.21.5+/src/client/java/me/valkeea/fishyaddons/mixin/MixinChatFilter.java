package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.processor.ChatProcessor;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;

@Mixin(ChatHud.class)
public class MixinChatFilter {

    private Text cachedFilteredMessage = null;

    @Inject(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cancelEmptyFilteredMessages(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        cachedFilteredMessage = ChatProcessor.getInstance().applyDisplayFilters(message);
        
        if (cachedFilteredMessage == null || cachedFilteredMessage.getString().trim().isEmpty()) {
            cachedFilteredMessage = null;
            ci.cancel();
        }
    }

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Text applyDisplayFiltersToMessage(Text message) {
        if (cachedFilteredMessage != null) {
            Text result = cachedFilteredMessage;
            cachedFilteredMessage = null;
            return result;
        }
        return message;
    }    
}
