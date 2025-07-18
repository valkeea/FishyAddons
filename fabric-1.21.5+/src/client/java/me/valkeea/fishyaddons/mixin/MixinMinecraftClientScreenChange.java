package me.valkeea.fishyaddons.mixin;

import me.valkeea.fishyaddons.hud.SearchHudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClientScreenChange {
    
    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onScreenChange(Screen screen, CallbackInfo ci) {
        SearchHudElement searchElement = SearchHudElement.getInstance();
        if (searchElement != null) {
            searchElement.onScreenChange();
        }
    }
}
