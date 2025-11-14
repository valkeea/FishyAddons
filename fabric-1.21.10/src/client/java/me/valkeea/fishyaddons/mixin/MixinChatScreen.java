package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.qol.CopyChat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Style;


@Mixin(ChatScreen.class)
public class MixinChatScreen {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        
        if (click.button() == 1 && CopyChat.isOn()) {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.currentScreen instanceof ChatScreenAccessor accessor) {
                Style style = accessor.invokeGetTextStyleAt(click.x(), click.y());
                if (style == null) return;
                CopyChat.tryCopyChat(click.x(), click.y());
                cir.setReturnValue(true);
            }  
        }
    }
}
