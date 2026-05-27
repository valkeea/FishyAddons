package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.qol.CopyChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;


@Mixin(ChatScreen.class)
public class MixinChatScreen {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        
        if (click.button() == 1 && CopyChat.isOn()) {
            var mc = Minecraft.getInstance();

            if (mc.screen instanceof ChatScreen) {
                CopyChat.tryCopyChat(mc, click.x(), click.y());
                cir.setReturnValue(true);
            }  
        }
    }
}
