package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.feature.qol.ItemSearchOverlay;
import me.valkeea.fishyaddons.hud.ui.SearchHudElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

@Mixin(HandledScreen.class)
public class MixinHandledScreenSearchOverlay {
    
    @Inject(method = "render", at = @At("TAIL"))
    private void renderSearchOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ItemSearchOverlay.getInstance().render(context, screen);
        SearchHudElement searchElement = SearchHudElement.getInstance();

        if (searchElement != null) {
            searchElement.render(context, mouseX, mouseY);
        }
    }
}
