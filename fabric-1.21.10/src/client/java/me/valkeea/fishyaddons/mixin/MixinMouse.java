package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.MouseClickEvent;
import me.valkeea.fishyaddons.event.impl.MouseScrollEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.util.Window;

@Mixin(Mouse.class)
public class MixinMouse {

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onMouseScrolled(long window, double horizontal, double vertical, CallbackInfo ci) {

        var mc = MinecraftClient.getInstance();

        double windowX = ((MouseAccessor)this).getX();
        double windowY = ((MouseAccessor)this).getY();
        
        double scaledX = windowX * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double scaledY = windowY * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        
        var event = new MouseScrollEvent(vertical, scaledX, scaledY);
        FaEvents.MOUSE_SCROLL.firePhased(event, listener -> listener.onScroll(event));
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        if (action != 1) return;
        
        var mc = MinecraftClient.getInstance();
        if (!(mc.currentScreen instanceof HandledScreen)) return;

        Window w = mc.getWindow();
        double windowX = ((MouseAccessor)this).getX();
        double windowY = ((MouseAccessor)this).getY();
        double scaledX = windowX * w.getScaledWidth() / w.getWidth();
        double scaledY = windowY * w.getScaledHeight() / w.getHeight();

        var click = new Click(scaledX, scaledY, input);
        var event = new MouseClickEvent(click);
        
        FaEvents.MOUSE_CLICK.firePhased(event, listener -> listener.onClick(event));
    }    
}
