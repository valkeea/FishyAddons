package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.skyblock.TransLava;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

@Mixin(FluidStateModelSet.class)
public abstract class MixinFluidStateModelSet {
    @Unique
    private FluidModel transLavaModel;

    @Unique
    private FluidModel vanillaWaterModel;

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void replaceModel(FluidState fluidState, CallbackInfoReturnable<FluidModel> cir) {
        var type = fluidState.getType();

        if (TransLava.isEnabled() && (type == Fluids.LAVA || type == Fluids.FLOWING_LAVA)) {
            FluidModel m = ((FluidStateModelSet) (Object) this).get(Fluids.WATER.defaultFluidState());

            if (transLavaModel == null || vanillaWaterModel != m) {
                vanillaWaterModel = m;
                transLavaModel = new FluidModel(
                    m.layer(),
                    m.stillMaterial(),
                    m.flowingMaterial(),
                    m.overlayMaterial(),
                    tint -> TransLava.getColor()
                );
            }

            cir.setReturnValue(transLavaModel);
        }
    }
}
