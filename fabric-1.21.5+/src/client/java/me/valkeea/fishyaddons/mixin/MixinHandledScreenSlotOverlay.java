package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.safeguard.SlotProtectionManager;
import me.valkeea.fishyaddons.tool.FishyMode;
import me.valkeea.fishyaddons.util.HelpUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;


@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenSlotOverlay {

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void drawSlotOverlay(DrawContext context, Slot slot, CallbackInfo ci) {

        if (!HelpUtil.isPlayerInventory()) return;
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        int invIndex = SlotProtectionManager.remap(screen, slot.id);

        if (invIndex <= 8 && invIndex >= 45) {
            return;
        }

        if (SlotProtectionManager.isSlotLocked(invIndex)) {
            overlay(context, slot, "falocked");
        }

        else if (SlotProtectionManager.isSlotBound(invIndex)) {
           overlay(context, slot, "fabound");
        }
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