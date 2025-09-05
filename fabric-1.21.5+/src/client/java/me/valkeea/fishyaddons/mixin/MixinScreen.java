package me.valkeea.fishyaddons.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.handler.FaColors;
import me.valkeea.fishyaddons.safeguard.SellProtectionHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;


@Mixin(Screen.class)
public abstract class MixinScreen {

    @Inject(
        method = "getTooltipFromItem",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onGetTooltipFromItem(
            MinecraftClient cl,
            ItemStack stack,
            CallbackInfoReturnable<List<Text>> cir
    ) {
        List<Text> originalTooltip = cir.getReturnValue();
        if (originalTooltip == null || originalTooltip.isEmpty()) return;
        if (ItemConfig.isTooltipEnabled() &&
            SellProtectionHandler.isProtectedCached(stack, cl.world != null ? cl.world.getRegistryManager() : null)) {
            int insertAt = originalTooltip.size();
            for (int i = 0; i < originalTooltip.size(); i++) {
                if (originalTooltip.get(i).getString().startsWith("NBT:")) {
                    insertAt = i;
                    break;
                }
            }
            originalTooltip.add(insertAt, Text.literal("§8§oFA Guarded"));
        }

        List<Text> finalTooltip = originalTooltip;
        if (FaColors.shouldColor()) {
        List<Text> recoloredTooltip = new ArrayList<>(originalTooltip.size());
        for (Text line : originalTooltip) {
            recoloredTooltip.add(FaColors.tooltipCached(line));
        }

        finalTooltip = recoloredTooltip;
        }
        cir.setReturnValue(finalTooltip);
    }
}