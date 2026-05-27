package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.skyblock.CocoonAlert;
import me.valkeea.fishyaddons.feature.skyblock.FishingHotspot;
import me.valkeea.fishyaddons.tracker.profit.ValuableMobs;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

@Mixin(ClientLevel.class)
public class MixinClientWorld {

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void onEntityAdd(Entity entity, CallbackInfo ci) {
        ValuableMobs.onEntityAdded(entity);
        CocoonAlert.onEntityAdded(entity);
    }    
    
    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void onEntityRemove(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        var entity = ((ClientLevel) (Object) this).getEntity(entityId);
        if (entity != null) {
            FishingHotspot.onEntityRemoved(entity);
        }
    }
}
