package me.valkeea.fishyaddons.mixin;

import me.valkeea.fishyaddons.handler.ItemSearchOverlay;
import me.valkeea.fishyaddons.hud.SearchHudElement;
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
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        ItemSearchOverlay.getInstance().render(context, screen);
        SearchHudElement searchElement = SearchHudElement.getInstance();
        if (searchElement != null) {
            searchElement.render(context, mouseX, mouseY);
        }
    }
}
