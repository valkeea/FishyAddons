package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.item.safeguard.SlotHandler;
import me.valkeea.fishyaddons.feature.skyblock.GuiIcons;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.SbGui;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenSlotOverlay {

    private static final ThreadLocal<Slot> CURRENT_SLOT = new ThreadLocal<>();

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void onDrawSlotHead(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        CURRENT_SLOT.set(slot);
    }

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void drawSlotOverlay(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {

        if (!SbGui.isPlayerInventory()) return;
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        int invIndex = SlotHandler.remap(screen, slot.id);

        if (invIndex <= 8 || invIndex >= 44) return;

        if (SlotHandler.isSlotLocked(invIndex)) {
            overlay(context, slot, "falocked");

        } else if (SlotHandler.isSlotBound(invIndex)) {
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
        var slot = CURRENT_SLOT.get();
        if (slot != null && GuiIcons.isBlocked(slot.id)) {
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
        var slot = CURRENT_SLOT.get();
        if (slot != null && GuiIcons.isBlocked(slot.id)) {
            return;
        }
        context.drawItemWithoutEntity(stack, x, y, seed);
    }

    @Inject(method = "drawSlot", at = @At("RETURN"))
    private void onDrawSlotReturn(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        CURRENT_SLOT.remove();
    }    

    private void overlay(DrawContext context, Slot slot, String textureName) {
        var mode = FishyMode.getTheme();
        var texture = Identifier.of("fishyaddons", "textures/gui/" + mode + "/" + textureName + ".png");
        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            texture,
            slot.x, slot.y,
            0.0F, 0.0F,
            16, 16,
            16, 16
        );
    }
}
