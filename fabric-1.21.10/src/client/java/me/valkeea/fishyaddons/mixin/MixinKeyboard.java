package me.valkeea.fishyaddons.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.ui.list.ScRules;
import me.valkeea.fishyaddons.ui.screen.AlertEditScreen;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;

@Mixin(Keyboard.class)
public abstract class MixinKeyboard {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void blockF3(long window, int action, KeyInput input, CallbackInfo ci) {

        if (window != client.getWindow().getHandle()) return;

        var screen = client.currentScreen;
        if (!(screen instanceof ScRules) && !(screen instanceof AlertEditScreen)) return;
        if (input.key() != GLFW.GLFW_KEY_F3) return;

        if (action == GLFW.GLFW_PRESS) {
            client.currentScreen.keyPressed(new KeyInput(input.key(), input.scancode(), input.modifiers()));
        }

        ci.cancel();
    }
}
