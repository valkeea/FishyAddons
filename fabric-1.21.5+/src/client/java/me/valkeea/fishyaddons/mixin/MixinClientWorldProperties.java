package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.skyblock.WeatherTracker;

@Mixin(targets = "net.minecraft.client.world.ClientWorld$Properties")
public class MixinClientWorldProperties {
    
    @Inject(method = "setRaining", at = @At("TAIL"))
    private void onRainStateChanged(boolean raining, CallbackInfo ci) {
        WeatherTracker.onRainStateChange(!raining);
    }
}
