package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.safeguard.SellProtectionHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenMouseRelease {

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        ItemStack cursorStack = mc.player.currentScreenHandler.getCursorStack();
        if (cursorStack == null || cursorStack.isEmpty()) return;

        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int guiWidth = (screen.width - accessor.getBackgroundWidth()) / 2;
        int guiHeight = (screen.height - accessor.getBackgroundHeight()) / 2;

        boolean outsideGui = mouseX < guiWidth|| mouseX > guiWidth + accessor.getBackgroundWidth() || 
        mouseY < guiHeight || mouseY > guiHeight + accessor.getBackgroundHeight();

        boolean notOverSlot = accessor.getFocusedSlot() == null;        

        if (SellProtectionHandler.isProtectedCached(cursorStack) &&
            outsideGui && notOverSlot) {
            SellProtectionHandler.triggerProtection();
            cir.setReturnValue(false);
        }
    }
}