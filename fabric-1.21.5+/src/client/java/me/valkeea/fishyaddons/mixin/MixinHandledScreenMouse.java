package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.hud.TrackerDisplay;
import me.valkeea.fishyaddons.safeguard.BlacklistMatcher;
import me.valkeea.fishyaddons.safeguard.SellProtectionHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenMouse extends Screen {
    protected MixinHandledScreenMouse(String titleString) {
        super(Text.literal(titleString));
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;

        TrackerDisplay profitTracker = TrackerDisplay.getInstance();
        if (profitTracker != null && profitTracker.handleMouseClick(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            return;
        }

        Slot hoveredSlot = ((HandledScreenAccessor) screen).getFocusedSlot();
        ItemStack stack;

        if (hoveredSlot != null) {
            stack = hoveredSlot.getStack();
        } else {
            stack = mc.player.currentScreenHandler.getCursorStack();
        }

        if (stack == null || stack.isEmpty()) return;
        RegistryWrapper.WrapperLookup registries = mc.world != null ? mc.world.getRegistryManager() : null;
        if (!SellProtectionHandler.isProtectedCached(stack, registries)) return;

        if (BlacklistMatcher.isBlacklistedGUI(screen, screen.getClass().getName()) && 
            ItemConfig.isSellProtectionEnabled()) {
            SellProtectionHandler.triggerProtection();
            cir.setReturnValue(false);
        }
    }
}
