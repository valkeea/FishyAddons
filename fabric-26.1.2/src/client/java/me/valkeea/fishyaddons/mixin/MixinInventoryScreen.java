package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.core.ScreenRenderContext;
import me.valkeea.fishyaddons.hud.ui.EqDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;

@Mixin(InventoryScreen.class)
public class MixinInventoryScreen {
    
    @Inject(
        method = "extractLabels",
        at = @At("TAIL")
    )
    private void onDrawForeground(GuiGraphicsExtractor gge, int mouseX, int mouseY, CallbackInfo ci) {
        var screen = (InventoryScreen) (Object) this;
        EqDisplay.render(gge, screen);
    }

    @Inject(
        method = "mouseReleased",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onMouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        
        var screen = (InventoryScreen) (Object) this;
        var accessor = (HandledScreenAccessor) screen;

        int guiLeft = accessor.getX();
        int guiTop = accessor.getY();
        
        double relativeMouseX = click.x() - guiLeft;
        double relativeMouseY = click.y() - guiTop;

        var display = EqDisplay.getInstance();
        if (display.isMouseOver(relativeMouseX, relativeMouseY) && display.handleMouseClick(click.button())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "extractRenderState",
        at = @At("RETURN")
    )
    private void onRender(GuiGraphicsExtractor gge, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        ScreenRenderContext.updateHoverState(mouseX, mouseY, ElementRegistry.getInteractiveElements());
        ScreenRenderContext.renderHoveredElements(gge, Minecraft.getInstance());
    }    
}
