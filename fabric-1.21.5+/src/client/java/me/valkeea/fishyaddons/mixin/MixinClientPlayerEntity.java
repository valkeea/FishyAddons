package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.safeguard.ItemHandler;
import me.valkeea.fishyaddons.safeguard.SellProtectionHandler;
import me.valkeea.fishyaddons.safeguard.SlotProtectionManager;
import me.valkeea.fishyaddons.util.ZoneUtils;
import net.minecraft.client.network.ClientPlayerEntity;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {

    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {

        if (ZoneUtils.isInDungeon()) {
            return;
        }

        var player = (ClientPlayerEntity) (Object) this;
        var stack = player.getMainHandStack();
        int selectedSlot = ((PlayerInventoryAccessor) player.getInventory()).getSelectedSlot();
        int guiSlotId = 36 + selectedSlot;

        if (SlotProtectionManager.isSlotLocked(guiSlotId) || SlotProtectionManager.isSlotBound(guiSlotId)) {
            cir.setReturnValue(false);
            return;
        }

        if (ItemHandler.isProtected(stack)) {
            cir.setReturnValue(false);
            SellProtectionHandler.triggerProtection();
        }
    }
}
