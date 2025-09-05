package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import me.valkeea.fishyaddons.handler.FaColors;
import me.valkeea.fishyaddons.tool.ModCheck;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * Modifies labels by replacing color styling.
 * Conditionally disabled to prevent outside flattening of modern formatting
 * resulting in missing style.
 */

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer {

    @ModifyVariable(
        method = "renderLabelIfPresent",
        at = @At("HEAD"),
        argsOnly = true,
        index = 2
    )
    private Text rewriteLabelText(Text text,
        PlayerEntityRenderState state,
        Text origText,
        MatrixStack matrixStack,
        VertexConsumerProvider vp,
        int i
    ) {
        if (FaColors.shouldColor() && !ModCheck.hasSh()) {
            return FaColors.first(text);
        }
        return text;
    }
}