package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.safeguard.ItemHandler;
import me.valkeea.fishyaddons.safeguard.SellProtectionHandler;
import me.valkeea.fishyaddons.safeguard.SlotProtectionManager;
import me.valkeea.fishyaddons.util.ZoneUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        // Skip logic if in dungeon
        if (ZoneUtils.isInDungeon()) {
            return;
        }

        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        ItemStack stack = player.getMainHandStack();

        // Prevent drop if hotbar slot is locked or bound
        int selectedSlot = ((PlayerInventoryAccessor) player.getInventory()).getSelectedSlot();
        int guiSlotId = 36 + selectedSlot; // Map hotbar index to GUI slot ID

        if (SlotProtectionManager.isSlotLocked(guiSlotId) || SlotProtectionManager.isSlotBound(guiSlotId)) {
            cir.setReturnValue(false); // Cancel drop, no notification
            return;
        }

        // UUID-based logic
        RegistryWrapper.WrapperLookup registries = MinecraftClient.getInstance().world.getRegistryManager();
        if (ItemHandler.isProtected(stack, registries)) {
            cir.setReturnValue(false); // Cancel drop
            SellProtectionHandler.triggerProtection();
        }
    }
}
