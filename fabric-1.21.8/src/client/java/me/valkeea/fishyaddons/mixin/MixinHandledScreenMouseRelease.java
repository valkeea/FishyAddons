package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.item.safeguard.GuiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenMouseRelease {

    @Inject(
        method = "mouseReleased",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        var mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        var cursorStack = mc.player.currentScreenHandler.getCursorStack();
        if (cursorStack == null || cursorStack.isEmpty()) return;

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int guiWidth = (screen.width - accessor.getBackgroundWidth()) / 2;
        int guiHeight = (screen.height - accessor.getBackgroundHeight()) / 2;

        boolean outsideGui = mouseX < guiWidth|| mouseX > guiWidth + accessor.getBackgroundWidth() || 
        mouseY < guiHeight || mouseY > guiHeight + accessor.getBackgroundHeight();

        boolean notOverSlot = accessor.getFocusedSlot() == null;        

        if (GuiHandler.isProtectedCached(cursorStack) &&
            outsideGui && notOverSlot) {
            GuiHandler.triggerProtection();
            cir.setReturnValue(false);
        }
    }
}
