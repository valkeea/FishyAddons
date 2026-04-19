package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.item.safeguard.FGUtil;
import me.valkeea.fishyaddons.util.ZoneUtils;
import net.minecraft.client.network.ClientPlayerEntity;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        if (ZoneUtils.isInDungeon()) return;

        var player = (ClientPlayerEntity) (Object) this;

        int selectedSlot = ((PlayerInventoryAccessor) player.getInventory()).getSelectedSlot();
        int slotIdx = 36 + selectedSlot;

        if (FGUtil.preventSlotClick(slotIdx)) {
            cir.setReturnValue(false);
            return;
        }

        var stack = player.getMainHandStack();

        if (FGUtil.isProtected(stack)) {
            cir.setReturnValue(false);
            FGUtil.triggerProtection();
        }
    }
}
