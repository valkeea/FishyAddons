package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.skyblock.CatchAlert;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.world.entity.projectile.FishingHook;

@Mixin(FishingHookRenderer.class)
public class MixinFishingBobberEntityRenderer {

    @Inject(
        method = "extractRenderState",
        at = @At("HEAD")
    )
    private void onUpdateRenderState(FishingHook entity, FishingHookRenderState state, float tickDelta, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        var owner = entity.getPlayerOwner();
        
        if (mc.player != null && owner == mc.player) {
            CatchAlert.onBobberRendered();
        }
    }
}
