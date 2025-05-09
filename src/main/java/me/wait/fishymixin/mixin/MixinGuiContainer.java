package me.wait.fishymixin.mixin;

import me.wait.fishyaddons.util.FishyNotis;
import me.wait.fishyaddons.tool.GuiClick;
import me.wait.fishyaddons.util.PlaySound;
import me.wait.fishyaddons.handlers.ProtectedItemHandler;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {


    /// Inject to conditionally cancel mouse click events in the GUI + register SlotClickEvent for SellProtection  
    @Inject(method = "func_146984_a", at = @At("HEAD"), cancellable = true)
    public void onHandleMouseClick(Slot slotIn, int slotId, int clickedButton, int clickType, CallbackInfo ci) {

        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiContainer)) return;

        ItemStack stack = (slotIn != null) ? slotIn.getStack() : Minecraft.getMinecraft().thePlayer.inventory.getItemStack();

        boolean clickingOutside = slotIn == null;
        boolean isThrowKey = clickType == 4 || clickType == 5;
        boolean isDragDrop = clickingOutside && (clickedButton == 0 || clickedButton == 1);

        GuiContainer gui = (GuiContainer) Minecraft.getMinecraft().currentScreen;
        GuiClick.SlotClickEvent event = new GuiClick.SlotClickEvent(gui, gui.inventorySlots, slotIn, slotId, clickedButton, clickType);
        boolean canceled = MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            ci.cancel();
            FishyNotis.protectNotification();
        }
        
        if ((isDragDrop || isThrowKey) && ProtectedItemHandler.isProtected(stack)) {
            ci.cancel();
            PlaySound.playProtectTrigger();
            FishyNotis.protectNotification();
        }
    }
}
