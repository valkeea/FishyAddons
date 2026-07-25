package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.item.safeguard.FGUtil;
import me.valkeea.fishyaddons.feature.item.safeguard.GuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreenMouseRelease {

    @Inject(
        method = "mouseReleased",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onMouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var stack = mc.player.containerMenu.getCarried();
        if (stack.isEmpty()) return;

        var screen = (AbstractContainerScreen<?>) (Object) this;
        var accessor = (HandledScreenAccessor) screen;
        int guiWidth = (screen.width - accessor.getImageWidth()) / 2;
        int guiHeight = (screen.height - accessor.getImageHeight()) / 2;

        boolean outsideGui = click.x() < guiWidth || click.x() > guiWidth + accessor.getImageWidth() ||
        click.y() < guiHeight || click.y() > guiHeight + accessor.getImageHeight();

        boolean notOverSlot = accessor.getHoveredSlot() == null;        
        if (GuiHandler.isProtectedCached(stack) &&
            outsideGui && notOverSlot) {
            FGUtil.triggerProtection();
            cir.setReturnValue(false);
        }
    }
}
