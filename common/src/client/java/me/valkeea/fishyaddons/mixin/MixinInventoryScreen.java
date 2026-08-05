package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.hud.core.ElementRegistry;
import me.valkeea.fishyaddons.hud.core.ScreenRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

@Mixin(InventoryScreen.class)
public class MixinInventoryScreen {

    @Inject(
        method = "extractRenderState",
        at = @At("RETURN")
    )
    private void onRender(GuiGraphicsExtractor gge, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        ScreenRenderContext.updateHoverState(mouseX, mouseY, ElementRegistry.getInteractiveElements());
        ScreenRenderContext.renderHoveredElements(gge, Minecraft.getInstance());
    }    
}
