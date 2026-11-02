package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class MixinMinecraftClient {

    @Inject(method = "resizeGui", at = @At("RETURN"))
    private void onResolutionChanged(CallbackInfo ci) {
        ElementRegistry.clearAllCaches();
        ScreenManager.refreshCurrentScreen();
    }    
}
