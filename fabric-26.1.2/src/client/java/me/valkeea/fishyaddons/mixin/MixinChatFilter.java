package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.processor.ChatProcessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

@Mixin(ChatComponent.class)
public class MixinChatFilter {

    private ThreadLocal<Component> cachedFilteredMessage = ThreadLocal.withInitial(() -> null);

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void cancelEmptyFilteredMessages(Component message, MessageSignature ms, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        if (!Minecraft.getInstance().isSameThread()) return;
        
        var filteredMessage = ChatProcessor.getInstance().applyDisplayFilters(message);
        cachedFilteredMessage.set(filteredMessage);
        
        if (filteredMessage == null || filteredMessage.getString().trim().isEmpty()) {
            cachedFilteredMessage.remove();
            ci.cancel();
        }
    }

    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component applyDisplayFiltersToMessage(Component message) {

        if (Minecraft.getInstance().isSameThread()) {

            var result = cachedFilteredMessage.get();
            if (result != null) {
                cachedFilteredMessage.remove();
                return result;
            }
        }

        return message;
    }    
}
