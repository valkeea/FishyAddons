package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.hud.ui.SearchHudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClientScreenChange {
    
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onScreenChange(Screen screen, CallbackInfo ci) {
        
        var searchElement = SearchHudElement.getInstance();
        if (searchElement != null) {
            searchElement.onScreenChange();
        }
    }
}
