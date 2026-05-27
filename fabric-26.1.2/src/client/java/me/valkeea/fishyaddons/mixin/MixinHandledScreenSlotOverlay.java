package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.item.safeguard.FGUtil;
import me.valkeea.fishyaddons.feature.item.safeguard.SlotHandler;
import me.valkeea.fishyaddons.feature.skyblock.GuiIcons;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.ContainerScanner;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreenSlotOverlay {

    private static final ThreadLocal<Slot> CURRENT_SLOT = new ThreadLocal<>();

    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void setCurrentSlot(GuiGraphicsExtractor gge, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        CURRENT_SLOT.set(slot);
    }

    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void drawSlotOverlayHead(GuiGraphicsExtractor gge, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (!FGUtil.isKeyBound() || !ContainerScanner.isGuiOrInv()) return;
        
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        int invIndex = SlotHandler.remap(screen, slot.index);
        if (invIndex == -1) return;

        if (FGUtil.isSlotLocked(invIndex)) {
            overlay(gge, slot, "falocked");

        } else if (FGUtil.isSlotBound(invIndex)) {
           overlay(gge, slot, "fabound");
        }
    }

    @Redirect(
        method = "extractSlot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;III)V"
        )
    )
    private void blockDrawItem(GuiGraphicsExtractor gge, ItemStack stack, int x, int y, int seed) {
        var slot = CURRENT_SLOT.get();
        if (slot != null && GuiIcons.isBlocked(slot.index)) {
            return;
        }
        gge.item(stack, x, y, seed);
    }

    @Redirect(
        method = "extractSlot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;III)V"
        )
    )
    private void blockDrawItemWithoutEntity(GuiGraphicsExtractor gge, ItemStack stack, int x, int y, int seed) {
        var slot = CURRENT_SLOT.get();
        if (slot != null && GuiIcons.isBlocked(slot.index)) {
            return;
        }
        gge.fakeItem(stack, x, y, seed);
    }

    @Inject(method = "extractSlot", at = @At("RETURN"))
    private void onDrawSlotReturn(GuiGraphicsExtractor gge, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        CURRENT_SLOT.remove();
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
