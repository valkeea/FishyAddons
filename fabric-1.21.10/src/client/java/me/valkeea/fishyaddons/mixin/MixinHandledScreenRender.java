package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.core.ScreenRenderContext;
import me.valkeea.fishyaddons.hud.ui.GoalButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

@Mixin(HandledScreen.class)
public class MixinHandledScreenRender {
    
    @Inject(
        method = "render",
        at = @At("TAIL")
    )
    private void renderElements(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        ScreenRenderContext.updateHoverState(mouseX, mouseY, ElementRegistry.getInteractiveElements());
        
        var screen = (HandledScreen<?>) (Object) this;
        if (screen instanceof GenericContainerScreen gcs) GoalButton.render(context, gcs, mouseX, mouseY);
        
        ScreenRenderContext.renderHoveredElements(context, MinecraftClient.getInstance());
    }
}
