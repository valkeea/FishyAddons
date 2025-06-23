package me.wait.fishymixin.mixin;

import me.wait.fishyaddons.handlers.SellProtectionHandler;
import me.wait.fishyaddons.config.UUIDConfigHandler;
import me.wait.fishyaddons.fishyprotection.BlacklistMatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.client.config.GuiUtils;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SideOnly(Side.CLIENT)
@Mixin(GuiContainer.class)
public abstract class MixinTooltip {
    

    @Inject(method = "func_73863_a", at = @At("TAIL"))
    public void onDrawScreenPost(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!UUIDConfigHandler.isTooltipEnabled()) return;

        GuiContainer gui = (GuiContainer) (Object) this;
        Slot hoveredSlot = gui.getSlotUnderMouse();
        if (hoveredSlot != null && hoveredSlot.getHasStack()) {
            ItemStack stack = hoveredSlot.getStack();
            if (SellProtectionHandler.isProtectedCached(stack)) {
                Minecraft mc = Minecraft.getMinecraft();

                List<String> tooltip = stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);

                // Add under stripped "NBT:"
                for (int i = 0; i < tooltip.size(); i++) {
                    String strippedLine = BlacklistMatcher.stripColor(tooltip.get(i));
                    if (strippedLine.startsWith("NBT:")) {
                        tooltip.add(i - 2, "§8§oFA Guarded");
                        break;
                    }
                }

                // Use GuiUtils to render the tooltip
                GuiUtils.drawHoveringText(
                    tooltip,
                    mouseX,
                    mouseY,
                    gui.width,
                    gui.height,
                    -1,
                    mc.fontRendererObj
                );
            }
        }
    }
}
