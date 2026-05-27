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
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboard {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void blockF3(long window, int action, KeyEvent input, CallbackInfo ci) {

        if (window != minecraft.getWindow().handle()) return;

        var screen = minecraft.screen;
        if (!(screen instanceof ScRules) && !(screen instanceof AlertEditScreen)) return;
        if (input.key() != GLFW.GLFW_KEY_F3) return;

        if (action == GLFW.GLFW_PRESS) {
            minecraft.screen.keyPressed(new KeyEvent(input.key(), input.scancode(), input.modifiers()));
        }

        ci.cancel();
    }
}
