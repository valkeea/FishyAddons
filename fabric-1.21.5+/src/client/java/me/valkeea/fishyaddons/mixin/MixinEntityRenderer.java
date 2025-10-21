package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import me.valkeea.fishyaddons.handler.FaColors;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer<S extends EntityRenderState> {

    @ModifyVariable(
        method = "renderLabelIfPresent",
        at = @At("HEAD"),
        argsOnly = true,
        index = 2
    )
    private Text rewriteLabelText(Text text,
        S state,
        Text origText,
        MatrixStack matrixStack,
        VertexConsumerProvider vp,
        int i
    ) {
        if (FaColors.shouldColor()) {
            return FaColors.first(text);
        }
        return text;
    }
}
