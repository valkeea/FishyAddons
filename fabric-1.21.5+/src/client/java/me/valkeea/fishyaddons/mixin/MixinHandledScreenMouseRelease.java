package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.safeguard.SellProtectionHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenMouseRelease {
    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // If the cursor stack is not empty, and the mouse is outside the GUI, this is a drop attempt
        ItemStack cursorStack = mc.player.currentScreenHandler.getCursorStack();
        if (cursorStack == null || cursorStack.isEmpty()) return;

        // GUI size
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int guiWidth = (screen.width - accessor.getBackgroundWidth()) / 2;
        int guiHeight = (screen.height - accessor.getBackgroundHeight()) / 2;

        // Check if mouse is outside the GUI background
        boolean outsideGui = mouseX < guiWidth|| mouseX > guiWidth + accessor.getBackgroundWidth() || 
        mouseY < guiHeight || mouseY > guiHeight + accessor.getBackgroundHeight();

        // Check if not hovering any slot
        boolean notOverSlot = accessor.getFocusedSlot() == null;        

        RegistryWrapper.WrapperLookup registries = mc.world != null ? mc.world.getRegistryManager() : null;
        if (SellProtectionHandler.isProtectedCached(cursorStack, registries) &&
            outsideGui && notOverSlot) {
            SellProtectionHandler.triggerProtection();
            cir.setReturnValue(false);
        }
    }
}