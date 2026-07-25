package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.core.ScreenRenderContext;
import me.valkeea.fishyaddons.hud.ui.GoalButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

@Mixin(AbstractContainerScreen.class)
public class MixinHandledScreenRender {
    
    @Inject(
        method = "extractRenderState",
        at = @At("TAIL")
    )
    private void renderElements(GuiGraphicsExtractor gge, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        ScreenRenderContext.updateHoverState(mouseX, mouseY, ElementRegistry.getInteractiveElements());
        
        var screen = (AbstractContainerScreen<?>) (Object) this;
        if (screen instanceof ContainerScreen gcs) GoalButton.render(gge, gcs, mouseX, mouseY);
        
        ScreenRenderContext.renderHoveredElements(gge, Minecraft.getInstance());
    }
}
