package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.handler.FishingHotspot;
import me.valkeea.fishyaddons.tracker.ValuableMobs;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;

@Mixin(ClientWorld.class)
public class MixinClientWorld {

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void onEntityAdd(Entity entity, CallbackInfo ci) {
        ValuableMobs.onEntityAdded(entity);
    }    
    
    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void onEntityRemove(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        Entity entity = ((ClientWorld) (Object) this).getEntityById(entityId);
        if (entity != null) {
            FishingHotspot.onEntityRemoved(entity);
        }
    }
}
