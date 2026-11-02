package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.compat.McApi;
import me.valkeea.fishyaddons.feature.item.safeguard.FGUtil;
import me.valkeea.fishyaddons.util.ZoneUtils;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    
    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void onDropSelectedItem(LocalPlayer player, boolean entireStack, CallbackInfo ci) {
        if (ZoneUtils.isInDungeon() && !McApi.screenIsActive()) return;

        int selectedSlot = ((PlayerInventoryAccessor) player.getInventory()).getSelectedSlot();
        int slotIdx = 36 + selectedSlot;

        if (FGUtil.preventSlotClick(slotIdx)) {
            ci.cancel();
            return;
        }

        var stack = player.getMainHandItem();

        if (FGUtil.isProtected(stack)) {
            ci.cancel();
            FGUtil.triggerProtection();
        }
    }
}
