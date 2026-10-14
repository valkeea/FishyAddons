package me.valkeea.fishyaddons.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.skyblock.TransLava;
import me.valkeea.fishyaddons.feature.visual.ato.AreaTextureOverrides;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

@Mixin(FluidStateModelSet.class)
public abstract class MixinFluidStateModelSet {

    @Shadow @Final
    private Map<Fluid, FluidModel> modelByFluid;

    @Unique
    private FluidModel transLavaModel;

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void replaceModel(FluidState fluidState, CallbackInfoReturnable<FluidModel> cir) {
        var fluid = fluidState.getType();

        if (TransLava.isEnabled() && (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA)) {
            if (transLavaModel == null) {

                var water = modelByFluid.get(Fluids.WATER);
                transLavaModel = new FluidModel(
                    water.layer(),
                    water.stillMaterial(),
                    water.flowingMaterial(),
                    water.overlayMaterial(),
                    tint -> TransLava.getColor()
                );
            }

            cir.setReturnValue(transLavaModel);
            return; // Translava cancels any ATO wrapping
        }

        var original = modelByFluid.get(fluid);

        if (original != null) {
            var remapped = AreaTextureOverrides.remapFluidModel(original);
            if (remapped != null) {
                cir.setReturnValue(remapped);
            }
        }
    }
}
