package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.skyblock.CatchAlert;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.client.render.entity.state.FishingBobberEntityState;
import net.minecraft.entity.projectile.FishingBobberEntity;

@Mixin(FishingBobberEntityRenderer.class)
public class MixinFishingBobberEntityRenderer {

    @Inject(
        method = "updateRenderState",
        at = @At("HEAD")
    )
    private void onUpdateRenderState(FishingBobberEntity entity, FishingBobberEntityState state, float tickDelta, CallbackInfo ci) {
        var mc = MinecraftClient.getInstance();
        var owner = entity.getPlayerOwner();
        
        if (mc.player != null && owner == mc.player) {
            CatchAlert.onFishingLineRendered();
        }
    }
}
