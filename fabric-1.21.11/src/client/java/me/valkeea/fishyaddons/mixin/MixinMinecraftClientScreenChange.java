package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.ScreenCloseEvent;
import me.valkeea.fishyaddons.event.impl.ScreenOpenEvent;
import me.valkeea.fishyaddons.hud.core.ScreenRenderContext;
import me.valkeea.fishyaddons.hud.ui.SearchHudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClientScreenChange {
    
    @Shadow
    public Screen currentScreen;
    
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onScreenChange(Screen screen, CallbackInfo ci) {

        var wasRelevantScreen = currentScreen instanceof InventoryScreen || currentScreen instanceof GenericContainerScreen;
        
        if (screen == null && wasRelevantScreen) {
            Text title = currentScreen.getTitle();
            if (title != null) {
                var event = new ScreenCloseEvent(title);
                FaEvents.SCREEN_CLOSE.firePhased(event, listener -> listener.onScreenClose(event));
            }
            ScreenRenderContext.reset();
            return;
        } 

        var isInv = screen instanceof InventoryScreen;
        var newGcs = screen instanceof GenericContainerScreen gcs ? gcs : null;
        var relevantScreen = isInv || newGcs != null;
        
        if (!relevantScreen && wasRelevantScreen) {
            ScreenRenderContext.reset(); // Context reset on switch
        }

        if (newGcs != null && newGcs.getTitle() != null) { // New screen is a container
            var event = new ScreenOpenEvent(newGcs, newGcs.getTitle());
            FaEvents.SCREEN_OPEN.firePhased(event, listener -> listener.onScreenOpen(event));
        }

        if (SearchHudElement.getInstance() != null) {
            SearchHudElement.onScreenChange(relevantScreen);
        }        
    }

    @Inject(method = "onResolutionChanged", at = @At("HEAD"))
    private void onResolutionChanged(CallbackInfo ci) {
        me.valkeea.fishyaddons.hud.core.ElementRegistry.clearAllCaches();
    }    
}
