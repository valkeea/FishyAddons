package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.Window;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.MouseClickEvent;
import me.valkeea.fishyaddons.event.impl.MouseScrollEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

@Mixin(MouseHandler.class)
public class MixinMouse {

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void onMouseScrolled(long window, double horizontal, double vertical, CallbackInfo ci) {

        var mc = Minecraft.getInstance();

        double windowX = ((MouseAccessor)this).getX();
        double windowY = ((MouseAccessor)this).getY();
        
        Window w = mc.getWindow();
        double scaledX = windowX * w.getWidth() / (w.getGuiScale() * w.getScreenWidth());
        double scaledY = windowY * w.getHeight() / (w.getGuiScale() * w.getScreenHeight());
        
        var event = new MouseScrollEvent(vertical, scaledX, scaledY);
        FaEvents.MOUSE_SCROLL.firePhased(event, listener -> listener.onScroll(event));
    }

    @Inject(method = "onButton", at = @At("HEAD"))
    private void onMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        if (action != 1) return;
        if (!(McApi.isScreen(AbstractContainerScreen.class))) return;

        Window w = Minecraft.getInstance().getWindow();
        double windowX = ((MouseAccessor)this).getX();
        double windowY = ((MouseAccessor)this).getY();
        double scaledX = windowX * w.getWidth() / (w.getGuiScale() * w.getScreenWidth());
        double scaledY = windowY * w.getHeight() / (w.getGuiScale() * w.getScreenHeight());

        var click = new MouseButtonEvent(scaledX, scaledY, input);
        var event = new MouseClickEvent(click);
        
        FaEvents.MOUSE_CLICK.firePhased(event, listener -> listener.onClick(event));
    }    
}
