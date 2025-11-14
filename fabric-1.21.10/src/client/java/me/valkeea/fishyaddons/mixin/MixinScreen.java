package me.valkeea.fishyaddons.mixin;

import java.util.List;
import java.util.stream.IntStream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.config.ItemConfig;
import me.valkeea.fishyaddons.feature.item.safeguard.GuiHandler;
import me.valkeea.fishyaddons.feature.visual.FaColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

@SuppressWarnings("squid:S1118")
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
        if (ItemConfig.isTooltipEnabled() && GuiHandler.isProtectedCached(stack)) {
            
            int nbtIndex = IntStream.range(0, originalTooltip.size())
                .filter(i -> originalTooltip.get(i).getString().startsWith("NBT:"))
                .findFirst()
                .orElse(-1);

            originalTooltip.add(nbtIndex == -1 ? originalTooltip.size() : nbtIndex, Text.literal("§8§oFA Guarded"));
        }

        List<Text> finalTooltip = originalTooltip;

        if (FaColors.shouldColor()) {
            finalTooltip = originalTooltip.stream()
                .map(FaColors::tooltipCached)
                .toList();
        }
        
        cir.setReturnValue(finalTooltip);
    }
}
