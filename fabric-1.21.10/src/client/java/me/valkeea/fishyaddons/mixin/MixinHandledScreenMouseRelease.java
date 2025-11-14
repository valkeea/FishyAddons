package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.item.safeguard.GuiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenMouseRelease {

    @Inject(
        method = "mouseReleased",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onMouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {

        var mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        var stack = mc.player.currentScreenHandler.getCursorStack();
        if (stack == null || stack.isEmpty()) return;

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        var accessor = (HandledScreenAccessor) screen;
        int guiWidth = (screen.width - accessor.getBackgroundWidth()) / 2;
        int guiHeight = (screen.height - accessor.getBackgroundHeight()) / 2;

        boolean outsideGui = click.x() < guiWidth || click.x() > guiWidth + accessor.getBackgroundWidth() ||
        click.y() < guiHeight || click.y() > guiHeight + accessor.getBackgroundHeight();

        boolean notOverSlot = accessor.getFocusedSlot() == null;        
        if (GuiHandler.isProtectedCached(stack) &&
            outsideGui && notOverSlot) {
            GuiHandler.triggerProtection();
            cir.setReturnValue(false);
        }
    }
}
