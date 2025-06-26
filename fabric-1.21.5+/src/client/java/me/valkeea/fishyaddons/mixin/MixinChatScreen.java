package me.valkeea.fishyaddons.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.handler.CopyChat;


@Mixin(ChatScreen.class)
public class MixinChatScreen {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 1 && CopyChat.isOn()) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.currentScreen instanceof ChatScreenAccessor accessor) {
            Style style = accessor.invokeGetTextStyleAt(mouseX, mouseY);
            if (style == null) return;
            CopyChat.tryCopyChat(mouseX, mouseY);
            cir.setReturnValue(true);
            }  
        }
    }
}
