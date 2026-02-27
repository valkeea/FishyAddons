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

        if (screen == null && (currentScreen instanceof GenericContainerScreen || currentScreen instanceof InventoryScreen)) {
            Text title = currentScreen.getTitle();
            if (title != null) {
                var event = new ScreenCloseEvent(title);
                FaEvents.SCREEN_CLOSE.firePhased(event, listener -> listener.onScreenClose(event));
            }
            ScreenRenderContext.reset(); // Context reset on close
            return;
        } 

        var isInv = screen instanceof InventoryScreen;
        var isContainer = screen instanceof GenericContainerScreen;
        
        if (!isInv && !isContainer && (currentScreen instanceof InventoryScreen || currentScreen instanceof GenericContainerScreen)) {
            ScreenRenderContext.reset(); // Context reset on switch
        }

        if (isContainer && screen != null && screen.getTitle() != null) { // New screen is a container
            var event = new ScreenOpenEvent((GenericContainerScreen) (isContainer ? screen : null), screen.getTitle());
            FaEvents.SCREEN_OPEN.firePhased(event, listener -> listener.onScreenOpen(event));
        }

        if (SearchHudElement.getInstance() != null) {
            SearchHudElement.onScreenChange(isInv || isContainer);
        }        
    }
}
