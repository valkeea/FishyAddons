package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.core.ScreenRenderContext;
import me.valkeea.fishyaddons.hud.ui.EqDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;

@Mixin(InventoryScreen.class)
public class MixinInventoryScreen {
    
    @Inject(
        method = "drawForeground",
        at = @At("TAIL")
    )
    private void onDrawForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        var screen = (InventoryScreen) (Object) this;
        EqDisplay.render(context, screen);
    }

    @Inject(
        method = "mouseReleased",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onMouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
        
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
        method = "render",
        at = @At("RETURN")
    )
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        ScreenRenderContext.updateHoverState(mouseX, mouseY, ElementRegistry.getInteractiveElements());
        ScreenRenderContext.renderHoveredElements(context, MinecraftClient.getInstance());
    }    
}
