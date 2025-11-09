package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.hud.ui.EqDisplay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

@Mixin(InventoryScreen.class)
public class MixinInventoryScreen {
    
    @Inject(method = "drawForeground", at = @At("TAIL"))
    private void onDrawForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        EqDisplay.render(context, screen);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        
        InventoryScreen screen = (InventoryScreen) (Object) this;
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int guiLeft = accessor.getX();
        int guiTop = accessor.getY();
        double relativeMouseX = mouseX - guiLeft;
        double relativeMouseY = mouseY - guiTop;
        
        if (EqDisplay.getInstance().isMouseOver(relativeMouseX, relativeMouseY) && EqDisplay.getInstance().handleMouseClick(button)) {
            cir.setReturnValue(true);
        }
    }
}
