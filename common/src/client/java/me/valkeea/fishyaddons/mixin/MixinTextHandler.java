package me.valkeea.fishyaddons.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import me.valkeea.fishyaddons.feature.visual.FaColors;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

@Mixin(StringSplitter.class)
public abstract class MixinTextHandler {
    @Redirect(
        method = "splitLines(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/network/chat/Style;Ljava/util/function/BiConsumer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/chat/FormattedText;visit(Lnet/minecraft/network/chat/FormattedText$StyledContentConsumer;Lnet/minecraft/network/chat/Style;)Ljava/util/Optional;"
        )
    )
    private <T> Optional<T> redirectVisit(
        FormattedText inst,
        FormattedText.StyledContentConsumer<T> v,
        Style style
    ) {
        if (FaColors.shouldColor()) {
            return inst.visit((stylex, string) -> {
                FaColors.applyRecolorAll(string, stylex, v);
                return Optional.empty();
            }, style);
        }
        return inst.visit(v, style);
    }
}
