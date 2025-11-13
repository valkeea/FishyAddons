package me.valkeea.fishyaddons.mixin;

import me.valkeea.fishyaddons.feature.qol.ItemSearchOverlay;
import me.valkeea.fishyaddons.hud.ui.SearchHudElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public class MixinHandledScreenSearchOverlay {
    
    @Inject(method = "render", at = @At("TAIL"))
    private void renderSearchOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        
        var searchElement = SearchHudElement.getInstance();
        if (searchElement != null) {
            HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
            if (searchElement.isOverlayActive() && !searchElement.getSearchTerm().isEmpty()) {
                ItemSearchOverlay.getInstance().renderOverlay(context, screen, searchElement.getSearchTerm());
            }
            searchElement.render(context, mouseX, mouseY);
        }
    }
}
