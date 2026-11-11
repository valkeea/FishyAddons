package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import me.valkeea.fishyaddons.feature.item.safeguard.FGUtil;
import me.valkeea.fishyaddons.feature.item.safeguard.SlotHandler;
import me.valkeea.fishyaddons.feature.skyblock.SlotHider;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.ContainerScanner;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreenSlot {

    @Inject(
        method = "extractSlot", at = @At("HEAD")
    )
    private void faExtractSlot(GuiGraphicsExtractor gge, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (!(ContainerScanner.isGuiOrInv() && FGUtil.isKeyBound())) return;
        
        var screen = (AbstractContainerScreen<?>) (Object) this;
        int invIdx = SlotHandler.remap(screen, slot.index);
        if (invIdx == -1) return;

        if (FGUtil.isSlotLocked(invIdx)) overlay(gge, slot, "falocked");
        else if (FGUtil.isSlotBound(invIdx)) overlay(gge, slot, "fabound");
    }

    @SuppressWarnings("java:S107")
    @WrapWithCondition(
        method = "extractSlot",
        at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;III)V"),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;III)V")
        }
    )
    private boolean faShowItem(GuiGraphicsExtractor gge, ItemStack stack, int x, int y, int seed,
            GuiGraphicsExtractor slotGge, Slot slot, int mouseX, int mouseY) {
        return !SlotHider.isBlocked(slot.index);
    }

    private void overlay(GuiGraphicsExtractor gge, Slot slot, String textureName) {
        var mode = FishyMode.themeName();
        var texture = Identifier.fromNamespaceAndPath("fishyaddons", "textures/gui/" + mode + "/" + textureName + ".png");
        gge.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            slot.x, slot.y,
            0.0F, 0.0F,
            16, 16,
            16, 16
        );
    }
}
