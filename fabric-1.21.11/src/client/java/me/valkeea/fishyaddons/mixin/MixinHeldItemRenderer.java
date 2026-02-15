package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.item.animations.HeldItems;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {

    @ModifyVariable(method = "swingArm", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float modifySwingProgressInSwingArm(float swingProgress) {
        
        if (!HeldItems.isEnabled()) {
            return swingProgress;
        }

        float intensity = HeldItems.getSwingIntensity();
        return swingProgress * intensity;
    }

    // Swing axis movement transforms
    @ModifyArg(method = "swingArm", at = @At(value = "INVOKE", 
               target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"), 
               index = 0)
    private float modifySwingTranslateX(float x) {
        if (!HeldItems.isEnabled()) {
            return x;
        }
        return x * HeldItems.getSwingXMovement();
    }

    @ModifyArg(method = "swingArm", at = @At(value = "INVOKE", 
               target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"), 
               index = 1)
    private float modifySwingTranslateY(float y) {
        if (!HeldItems.isEnabled()) {
            return y;
        }
        return y * HeldItems.getSwingYMovement();
    }

    @ModifyArg(method = "swingArm", at = @At(value = "INVOKE", 
               target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"), 
               index = 2)
    private float modifySwingTranslateZ(float z) {
        if (!HeldItems.isEnabled()) {
            return z;
        }
        return z * HeldItems.getSwingZMovement();
    }    

    // Equip animation  
    @ModifyArg(method = "renderFirstPersonItem", at = @At(value = "INVOKE", 
               target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"),
               index = 2)
    private float modifyApplyEquipOffsetProgress(float equipProgress) {
        if (!HeldItems.isEnabled()) {
            return equipProgress;
        }
        return equipProgress * HeldItems.getEquipIntensity();
    }

    // Scale / rotation / position transforms
    @SuppressWarnings("squid:S107")    
    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", 
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"), cancellable = true)
    private void onRenderItem(
        AbstractClientPlayerEntity player,
        float tickProgress,
        float pitch,
        Hand hand,
        float swingProgress,
        ItemStack item,
        float equipProgress,
        MatrixStack matrices,
        OrderedRenderCommandQueue orderedRenderCommandQueue,
        int light, 
        CallbackInfo ci) {
        
        HeldItems.applyAllTransformations(matrices, hand);
    }
}
