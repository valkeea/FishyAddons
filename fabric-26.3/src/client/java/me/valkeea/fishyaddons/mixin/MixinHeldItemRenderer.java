package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import me.valkeea.fishyaddons.feature.item.animations.HeldItems;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class MixinHeldItemRenderer {

    @ModifyVariable(method = "swingArm", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float modifySwingProgressInSwingArm(float swingProgress) {
        if (!HeldItems.isEnabled()) {
            return swingProgress;
        }

        return (float) (swingProgress * HeldItems.getSwingIntensity());
    }

    // Swing axis movement transforms
    @ModifyArg(method = "swingArm", at = @At(value = "INVOKE", 
               target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), 
               index = 0)
    private float modifySwingTranslateX(float x) {
        if (!HeldItems.isEnabled()) {
            return x;
        }
        return (float) (x * HeldItems.getSwingXMovement());
    }

    @ModifyArg(method = "swingArm", at = @At(value = "INVOKE", 
               target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), 
               index = 1)
    private float modifySwingTranslateY(float y) {
        if (!HeldItems.isEnabled()) {
            return y;
        }
        return (float) (y * HeldItems.getSwingYMovement());
    }

    @ModifyArg(method = "swingArm", at = @At(value = "INVOKE", 
               target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), 
               index = 2)
    private float modifySwingTranslateZ(float z) {
        if (!HeldItems.isEnabled()) {
            return z;
        }
        return (float) (z * HeldItems.getSwingZMovement());
    }    

    // Equip animation  
    @ModifyArg(
        method = "submitArmWithItem",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"),
        index = 2
    )
    private float modifyApplyEquipOffsetProgress(float equipProgress) {
        if (!HeldItems.isEnabled()) {
            return equipProgress;
        }
        return (float) (equipProgress * HeldItems.getEquipIntensity());
    }

    // Scale / rotation / position transforms, applied right before the held item is submitted
    @SuppressWarnings("squid:S107")
    @Inject(
        method = "submitArmWithItem",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V")
    )
    private void onSubmitItem(
        PlayerRenderState playerState,
        FirstPersonHandsAndItemsRenderState state,
        float partialTicks,
        float xRot,
        InteractionHand hand,
        float attack,
        ItemStack itemStack,
        float inverseArmHeight,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        CallbackInfo ci) {

        HeldItems.applyAllTransformations(poseStack, hand);
    }
}