package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.ScreenCloseEvent;
import me.valkeea.fishyaddons.event.impl.ScreenOpenEvent;
import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.core.ScreenRenderContext;
import me.valkeea.fishyaddons.hud.ui.SearchHudElement;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

@Mixin(Minecraft.class)
public class MixinMinecraftClientScreenChange {
    
    @Shadow
    public Screen screen;
    
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onScreenChange(Screen newScreen, CallbackInfo ci) {

        var wasRelevantScreen = screen instanceof InventoryScreen || screen instanceof ContainerScreen;
        
        if (newScreen == null && wasRelevantScreen) {
            var oldTitle = screen.getTitle();
            var event = new ScreenCloseEvent(oldTitle);
            FaEvents.SCREEN_CLOSE.firePhased(event, listener -> listener.onScreenClose(event));
            ScreenRenderContext.reset();
            return;
        } 

        var isInv = newScreen instanceof InventoryScreen;
        var newCs = newScreen instanceof ContainerScreen cs ? cs : null;
        var relevantScreen = isInv || newCs != null;
        
        if (!relevantScreen && wasRelevantScreen) {
            ScreenRenderContext.reset(); // Context reset on switch
        }

        if (newCs != null) { // New screen is a container
            var event = new ScreenOpenEvent(newCs, newCs.getTitle());
            FaEvents.SCREEN_OPEN.firePhased(event, listener -> listener.onScreenOpen(event));
        }

        if (SearchHudElement.getInstance() != null) {
            SearchHudElement.onScreenChange(relevantScreen);
        }        
    }

    @Inject(method = "resizeGui", at = @At("HEAD"))
    private void onResolutionChanged(CallbackInfo ci) {
        ElementRegistry.clearAllCaches();
        ScreenManager.refreshCurrentScreen();
    }    
}
