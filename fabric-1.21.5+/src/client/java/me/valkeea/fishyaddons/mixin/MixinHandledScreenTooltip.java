package me.valkeea.fishyaddons.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.safeguard.SellProtectionHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

@Mixin(HandledScreen.class)
public abstract class MixinHandledScreenTooltip {

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void onDrawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        Slot slot = ((HandledScreenAccessor) screen).getFocusedSlot();
        if (slot != null && slot.hasStack()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            ItemStack stack = slot.getStack();
            RegistryWrapper.WrapperLookup registries = mc.world != null ? mc.world.getRegistryManager() : null;
            if (!SellProtectionHandler.isProtectedCached(stack, registries) ||
                !ItemConfig.isTooltipEnabled()) {
                return;
            }

            List<Text> tooltip = new ArrayList<>(Screen.getTooltipFromItem(mc, stack));
            int insertAt = tooltip.size();
            for (int i = 0; i < tooltip.size(); i++) {
                if (tooltip.get(i).getString().startsWith("NBT:")) {
                    insertAt = i;
                    break;
                }
            }

            tooltip.add(insertAt, Text.literal("§8§oFA Guarded"));

            context.drawTooltip(screen.getTextRenderer(), tooltip, x, y);
            ci.cancel();
        }
    }
}