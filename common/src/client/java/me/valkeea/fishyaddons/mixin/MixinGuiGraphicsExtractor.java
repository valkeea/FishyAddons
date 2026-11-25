package me.valkeea.fishyaddons.mixin;

import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.vconfig.ui.screen.AdjustedScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Mixin(GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphicsExtractor {
    @Shadow @Final
    private Minecraft minecraft;

    @Shadow @Final
    private Matrix3x2fStack pose;

    @Shadow @Final
    public GuiGraphicsExtractor.ScissorStack scissorStack;

    @Inject(method = "containsPointInScissor", at = @At("HEAD"), cancellable = true)
    private void faScissorPoint(int x, int y, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.screen instanceof AdjustedScreen) {
            var p = pose.transformPosition(new Vector2f((float)x, (float)y));
            cir.setReturnValue(scissorStack.containsPoint(Math.round(p.x), Math.round(p.y)));
        }
    }
}
