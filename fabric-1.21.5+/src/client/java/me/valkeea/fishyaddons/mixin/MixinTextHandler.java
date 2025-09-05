package me.valkeea.fishyaddons.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import me.valkeea.fishyaddons.handler.FaColors;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;

@Mixin(TextHandler.class)
public abstract class MixinTextHandler {
    @Redirect(
        method = "wrapLines(Lnet/minecraft/text/StringVisitable;ILnet/minecraft/text/Style;Ljava/util/function/BiConsumer;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/text/StringVisitable;visit(Lnet/minecraft/text/StringVisitable$StyledVisitor;Lnet/minecraft/text/Style;)Ljava/util/Optional;"
        )
    )
    private <T> Optional<T> redirectVisit(
        StringVisitable inst,
        StringVisitable.StyledVisitor<T> v,
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
