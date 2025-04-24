package me.wait.fishymixin.mixin;

import me.wait.fishyaddons.handlers.ProtectedItemHandler;
import me.wait.fishyaddons.util.FishyNotis;
import me.wait.fishyaddons.util.PlaySound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//Simple drop protection
@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSPDrop {

    @Inject(method = "func_71040_bB", at = @At("HEAD"), cancellable = true)
    public void onDropItem(boolean dropAll, CallbackInfoReturnable<EntityItem> cir) {
        EntityPlayerSP player = (EntityPlayerSP)(Object)this;
        ItemStack stack = player.inventory.getCurrentItem();

        if (ProtectedItemHandler.isProtected(stack)) {
            cir.setReturnValue(null);
            PlaySound.playProtectTrigger();
            FishyNotis.protectNotification();
        }
    }
}
