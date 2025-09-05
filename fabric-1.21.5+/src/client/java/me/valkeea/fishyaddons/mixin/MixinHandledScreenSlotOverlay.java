package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.safeguard.SlotProtectionManager;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.HelpUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenSlotOverlay {

    private static final ThreadLocal<Slot> CURRENT_SLOT = new ThreadLocal<>();

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void onDrawSlotHead(DrawContext context, Slot slot, CallbackInfo ci) {
        CURRENT_SLOT.set(slot);
    }

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void drawSlotOverlay(DrawContext context, Slot slot, CallbackInfo ci) {

        if (!HelpUtil.isPlayerInventory()) return;
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        int invIndex = SlotProtectionManager.remap(screen, slot.id);

        if (invIndex <= 8 && invIndex >= 44) {
            return;
        }

        if (SlotProtectionManager.isSlotLocked(invIndex)) {
            overlay(context, slot, "falocked");
        }

        else if (SlotProtectionManager.isSlotBound(invIndex)) {
           overlay(context, slot, "fabound");
        }
    }

    @Redirect(
        method = "drawSlot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawItem(Lnet/minecraft/item/ItemStack;III)V"
        )
    )
    private void blockDrawItem(DrawContext context, ItemStack stack, int x, int y, int seed) {
        Slot slot = CURRENT_SLOT.get();
        if (slot != null && me.valkeea.fishyaddons.handler.GuiIcons.isBlocked(slot.id)) {
            return;
        }
        context.drawItem(stack, x, y, seed);
    }

    @Redirect(
        method = "drawSlot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawItemWithoutEntity(Lnet/minecraft/item/ItemStack;III)V"
        )
    )
    private void blockDrawItemWithoutEntity(DrawContext context, ItemStack stack, int x, int y, int seed) {
        Slot slot = CURRENT_SLOT.get();
        if (slot != null && me.valkeea.fishyaddons.handler.GuiIcons.isBlocked(slot.id)) {
            return;
        }
        context.drawItemWithoutEntity(stack, x, y, seed);
    }

    @Inject(method = "drawSlot", at = @At("RETURN"))
    private void onDrawSlotReturn(DrawContext context, Slot slot, CallbackInfo ci) {
        CURRENT_SLOT.remove();
    }    

    private void overlay(DrawContext context, Slot slot, String textureName) {
        String mode = FishyMode.getTheme();
        Identifier texture = Identifier.of("fishyaddons", "textures/gui/" + mode + "/" + textureName + ".png");
        context.drawTexture(
            RenderLayer::getGuiTextured,
            texture,
            slot.x, slot.y,
            0.0F, 0.0F,
            16, 16,
            16, 16
        );
    }
}