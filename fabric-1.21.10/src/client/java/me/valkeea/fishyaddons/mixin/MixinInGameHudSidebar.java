package me.valkeea.fishyaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.valkeea.fishyaddons.feature.visual.FaColors;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

@Mixin(Team.class)
public class MixinInGameHudSidebar {

    @Inject(method = "decorateName", at = @At("TAIL"))
    private void modifySidebarNameText(Text name, CallbackInfoReturnable<Text> cir) {
        FaColors.recolorSidebarText(name);

    }
}
