package me.valkeea.fishyaddons.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.safeguard.SellProtectionHandler;
import me.valkeea.fishyaddons.util.CustomStrings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;


@Mixin(Screen.class)
public abstract class MixinScreen {

    @Inject(
        method = "getTooltipFromItem",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onGetTooltipFromItem(
            MinecraftClient client,
            ItemStack stack,
            CallbackInfoReturnable<List<Text>> cir
    ) {
        List<Text> originalTooltip = cir.getReturnValue();
        if (originalTooltip == null || originalTooltip.isEmpty()) return;

        List<Text> recoloredTooltip = new ArrayList<>(originalTooltip.size());
        for (Text line : originalTooltip) {
            recoloredTooltip.add(CustomStrings.rewriteWithMultipleMatches(line));
        }

        if (ItemConfig.isTooltipEnabled() &&
            SellProtectionHandler.isProtectedCached(stack, client.world != null ? client.world.getRegistryManager() : null)) {
            int insertAt = recoloredTooltip.size();
            for (int i = 0; i < recoloredTooltip.size(); i++) {
                if (recoloredTooltip.get(i).getString().startsWith("NBT:")) {
                    insertAt = i;
                    break;
                }
            }

            recoloredTooltip.add(insertAt, Text.literal("§8§oFA Guarded"));
        }

        cir.setReturnValue(recoloredTooltip);
    }
}