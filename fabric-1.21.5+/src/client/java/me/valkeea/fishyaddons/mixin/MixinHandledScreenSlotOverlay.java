package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.safeguard.SlotProtectionManager;
import me.valkeea.fishyaddons.util.HelpUtil;
import me.valkeea.fishyaddons.gui.GuiUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;


@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenSlotOverlay {
    private static final Identifier LOCKED_OVERLAY = Identifier.of("fishyaddons", "gui/falocked");
    private static final Identifier BOUND_OVERLAY = Identifier.of("fishyaddons", "gui/fabound");

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void drawSlotOverlay(DrawContext context, Slot slot, CallbackInfo ci) {

        if (!HelpUtil.isPlayerInventory()) return;
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        int invIndex = SlotProtectionManager.remap(screen, slot.id);

        if (invIndex <= 8 && invIndex >= 45) {
            return;
        }

        if (SlotProtectionManager.isSlotLocked(invIndex)) {
            GuiUtil.lockedOverlay(context, slot.x, slot.y);
        }

        else if (SlotProtectionManager.isSlotBound(invIndex)) {
           GuiUtil.boundOverlay(context, slot.x, slot.y);
        }
    }

    // to-do
    private void overlay(DrawContext context, Slot slot, Identifier textureId) {      
        context.drawTexture(
            RenderLayer::getGuiTextured,
            textureId,
            slot.x, slot.y,
            0, 0,
            16, 16,
            16, 16
        );
    }    
}