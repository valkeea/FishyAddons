package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.skyblock.WeatherTracker;
import net.minecraft.world.level.Level;

@Mixin(Level.class)
public class MixinClientWorldProperties {
    
    @Inject(method = "setRainLevel", at = @At("TAIL"))
    private void onRainLevel(float rainLevel, CallbackInfo ci) {
        WeatherTracker.onRainLevelChange(rainLevel > 0.0F);
    }
}
